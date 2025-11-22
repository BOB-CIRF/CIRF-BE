package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.scan.entity.EnabledInstances;
import com.cirf.dashboard.domain.scan.exception.AwsEc2Exception;
import com.cirf.dashboard.domain.scan.exception.ErrorMessage;
import com.cirf.dashboard.domain.scan.exception.ScanEc2Exception;
import com.cirf.dashboard.domain.scan.repository.ScanEc2Repository;
import com.cirf.dashboard.domain.scan.service.enums.AwsRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncScanEc2Service {

    private final ScanEc2Repository scanEc2Repository;

    @Value("${aws.sts.role-name}")
    private String roleName;

    @Value("${aws.sts.session-name}")
    private String sessionName;

    @Value("${aws.sts.session-duration}")
    private Integer sessionDuration;

    @Async("taskExecutor")
    public void saveScanEc2Instances(long ec2ScanId, String accountId) {
        log.info("Starting async EC2 scan on thread: {} for accountId: {}", Thread.currentThread().getName(), accountId);

        // 1. 대상 계정의 credentials 획득 (AssumeRole)
        AwsCredentialsProvider credentialsProvider = assumeRole(accountId);

        // 2. 활성화된 리전 목록 조회 (비동기 내부에서 수행)
        List<Region> enabledRegions = getEnabledRegions(credentialsProvider);
        log.info("Found {} enabled regions", enabledRegions.size());

        // 3. 활성화된 리전에서 병렬로 EC2 인스턴스 스캔
        ExecutorService executorService = Executors.newFixedThreadPool(enabledRegions.size());

        try {
            List<CompletableFuture<List<EnabledInstances>>> futures = enabledRegions.stream()
                    .map(region -> CompletableFuture.supplyAsync(
                            () -> scanEc2InRegion(ec2ScanId, region, credentialsProvider),
                            executorService
                    ))
                    .toList();

            // 모든 리전의 결과를 수집
            List<EnabledInstances> allInstances = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            log.info("Found {} EC2 instances across all regions", allInstances.size());

            // DynamoDB에 저장
            if (!allInstances.isEmpty()) {
                scanEc2Repository.saveEnabledInstances(allInstances);
                log.info("Saved {} EC2 instances to DynamoDB", allInstances.size());
            }

        } catch (ScanEc2Exception e) {
            log.error("Error during EC2 scan", e);
            throw new ScanEc2Exception(ErrorMessage.FAILED_CREATE_EC2_REGION);
        } finally {
            executorService.shutdown();
        }
    }

    private List<EnabledInstances> scanEc2InRegion(Long ec2ScanId, Region region, AwsCredentialsProvider credentialsProvider) {
        List<EnabledInstances> instances = new ArrayList<>();

        try (Ec2Client ec2Client = Ec2Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build()) {

            log.info("Scanning EC2 instances in region: {}", region.id());

            // DescribeInstances 요청
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            // 각 Reservation의 인스턴스들을 처리
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    EnabledInstances enabledInstance = scanEc2Repository.createEnabledInstance(ec2ScanId, region.id(), instance);
                    instances.add(enabledInstance);
                }
            }

            log.info("Found {} instances in region: {}", instances.size(), region.id());

        } catch (AwsEc2Exception e) {
            log.error("AWS EC2 error scanning region: {}", region.id(), e);
            throw new AwsEc2Exception(ErrorMessage.AWS_EC2_API_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error scanning region: {}", region.id(), e);
            // 특정 리전 스캔 실패 시 빈 리스트 반환 (다른 리전 스캔은 계속 진행)
        }

        return instances;
    }

    private List<Region> getEnabledRegions(AwsCredentialsProvider credentialsProvider) {
        // 기본 리전에서 DescribeRegions 호출하여 활성화된 리전 목록 조회
        try (Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.US_EAST_1) // 기본 리전 사용
                .credentialsProvider(credentialsProvider)
                .build()) {

            DescribeRegionsRequest request = DescribeRegionsRequest.builder()
                    .allRegions(false) // 활성화된 리전만 조회
                    .build();

            DescribeRegionsResponse response = ec2Client.describeRegions(request);

            List<Region> enabledRegions = response.regions().stream()
                    .map(regionInfo -> Region.of(regionInfo.regionName()))
                    .toList();

            log.info("Enabled regions: {}", enabledRegions.stream()
                    .map(Region::id)
                    .collect(Collectors.joining(", ")));

            return enabledRegions;

        } catch (Exception e) {
            log.error("Error fetching enabled regions, falling back to all regions", e);
            // 에러 발생 시 모든 리전 목록 반환
            return AwsRegion.getAllRegions();
        }
    }

    /**
     * AWS STS AssumeRole을 사용하여 대상 계정의 임시 자격 증명 획득
     * @param accountId 대상 AWS 계정 ID
     * @return AwsCredentialsProvider 임시 자격 증명 제공자
     */
    private AwsCredentialsProvider assumeRole(String accountId) {
        String roleArn = String.format("arn:aws:iam::%s:role/%s", accountId, roleName);
        log.info("Attempting to assume role: {}", roleArn);

        try (StsClient stsClient = StsClient.builder()
                .region(Region.US_EAST_1)
                .build()) {

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(sessionName)
                    .durationSeconds(sessionDuration)
                    .build();

            AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            Credentials credentials = assumeRoleResponse.credentials();

            log.info("Successfully assumed role: {}", roleArn);

            // 임시 자격 증명으로 CredentialsProvider 생성
            AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                    credentials.accessKeyId(),
                    credentials.secretAccessKey(),
                    credentials.sessionToken()
            );

            return StaticCredentialsProvider.create(sessionCredentials);

        } catch (Exception e) {
            log.error("Failed to assume role: {}", roleArn, e);
            throw new ScanEc2Exception(ErrorMessage.FAILED_CREATE_EC2_REGION);
        }
    }
}
