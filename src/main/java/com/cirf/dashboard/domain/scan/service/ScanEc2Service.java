package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanCompletedResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanEc2Response;
import com.cirf.dashboard.domain.scan.entity.EnabledInstances;
import com.cirf.dashboard.domain.scan.entity.ScanEc2Metadata;
import com.cirf.dashboard.domain.scan.exception.*;
import com.cirf.dashboard.domain.scan.service.enums.AwsRegion;
import com.cirf.dashboard.domain.scan.repository.ScanEc2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanEc2Service {

    private final ScanEc2Repository scanEc2Repository;

    public ScanCompletedResponse scanEc2Request(long tenantId, long caseId, String accountId) {
        // 1. ScanEc2Metadata 생성
        ScanEc2Metadata metadata = scanEc2Repository.createScanEc2Metadata(tenantId, caseId, accountId)
                .orElseThrow(() -> new ScanEc2MetadataCreationException(ErrorMessage.FAILED_CREATE_EC2_METADATA));

        Long ec2ScanId = metadata.getScanId();
        log.info("Created EC2 scan with ID: {}", ec2ScanId);

        // 2. 활성화된 리전 목록 조회
        List<Region> enabledRegions = getEnabledRegions();
        log.info("Found {} enabled regions", enabledRegions.size());

        // 3. 활성화된 리전에서 병렬로 EC2 인스턴스 스캔
        ExecutorService executorService = Executors.newFixedThreadPool(enabledRegions.size());

        try {
            List<CompletableFuture<List<EnabledInstances>>> futures = enabledRegions.stream()
                    .map(region -> CompletableFuture.supplyAsync(
                            () -> scanEc2InRegion(ec2ScanId, region),
                            executorService
                    ))
                    .toList();

            // 4. 모든 리전의 결과를 수집
            List<EnabledInstances> allInstances = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            log.info("Found {} EC2 instances across all regions", allInstances.size());

            // 5. DynamoDB에 저장
            if (!allInstances.isEmpty()) {
                scanEc2Repository.saveEnabledInstances(allInstances);
                log.info("Saved {} EC2 instances to DynamoDB", allInstances.size());
            }

            return new ScanCompletedResponse(ec2ScanId);

        } catch (ScanEc2Exception e) {
            log.error("Error during EC2 scan", e);
            throw new ScanEc2Exception(ErrorMessage.FAILED_CREATE_EC2_REGION);
        } finally {
            executorService.shutdown();
        }
    }

    private List<Region> getEnabledRegions() {
        // 기본 리전에서 DescribeRegions 호출하여 활성화된 리전 목록 조회
        try (Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.US_EAST_1) // 기본 리전 사용
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

    private List<EnabledInstances> scanEc2InRegion(Long ec2ScanId, Region region) {
        List<EnabledInstances> instances = new ArrayList<>();

        try (Ec2Client ec2Client = Ec2Client.builder()
                .region(region)
                .build()) {

            log.info("Scanning EC2 instances in region: {}", region.id());

            // DescribeInstances 요청
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            // 각 Reservation의 인스턴스들을 처리
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    EnabledInstances enabledInstance = createEnabledInstance(ec2ScanId, region.id(), instance);
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

    private EnabledInstances createEnabledInstance(Long ec2ScanId, String region, Instance instance) {
        String instanceId = instance.instanceId();
        String instanceName = instance.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .map(Tag::value)
                .findFirst()
                .orElse("");

        String pk = "EC2#" + ec2ScanId;
        String sk = String.format("REG#%s#INSTANCE#%s", region, instanceId);

        return EnabledInstances.builder()
                .pk(pk)
                .sk(sk)
                .ec2ScanId(ec2ScanId)
                .instanceId(instanceId)
                .instanceName(instanceName)
                .instanceType(instance.instanceType().toString())
                .region(region)
                .status(instance.state().nameAsString())
                .publicIp(instance.publicIpAddress() != null ? instance.publicIpAddress() : "")
                .build();
    }

    public Slice<ScanEc2Response> getEc2Lists(long tenantId, long ec2ScanId, ScanResultsRequest request){
        // ScanEc2 정보 객체 검증
        ScanEc2Metadata ec2Metadata = scanEc2Repository.getEc2MetadataByEc2ScanId(ec2ScanId)
                .orElseThrow(() -> new NotFoundEc2MetadataException(ErrorMessage.EC2_METADATA_NOT_FOUND));

        // tenantId 검증 : ec2ScanId에 해당하는 metadata의 tenant_id와 일치하는가.
        if (ec2Metadata.getTenantId() != tenantId) {
            throw new CustomAccessDeniedException(ErrorMessage.CUSTOM_ACCESS_DENIED);
        }

        // accountId 검증
        if (!ec2Metadata.getAccountId().equals(request.accountId())){
            throw new CustomAccessDeniedException(ErrorMessage.CUSTOM_ACCESS_DENIED);
        }

        // region 검증
        if (!AwsRegion.isValidRegion(request.region())) {
            throw new InvalidRegionException(ErrorMessage.INVALID_REGION);
        }

        // region별 EC2 인스턴스 조회
        List<EnabledInstances> instances = scanEc2Repository.getEc2InstancesByRegion(ec2ScanId, request.region());

        log.info("Found {} instances for ec2ScanId: {}, region: {}", instances.size(), ec2ScanId, request.region());

        // EnabledInstances를 ScanEc2Response로 변환
        List<ScanEc2Response> ec2Responses = instances.stream()
                .map(instance -> new ScanEc2Response(
                        instance.getInstanceId(),
                        instance.getInstanceName(),
                        instance.getInstanceType(),
                        instance.getRegion(),
                        instance.getStatus(),
                        instance.getPublicIp()
                ))
                .toList();

        // 페이징 처리
        Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), ec2Responses.size());

        List<ScanEc2Response> pagedContent = ec2Responses.subList(start, end);
        boolean hasNext = end < ec2Responses.size();

        return new SliceImpl<>(pagedContent, pageable, hasNext);
    }
}
