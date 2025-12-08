package com.cirf.dashboard.domain.cases.service;

import com.cirf.dashboard.domain.cases.dto.response.StackInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudFormationStackService {

    private final StsClient stsClient;

    /**
     * CloudFormation Stack 정보 조회
     *
     * @param caseId 사례 ID
     * @param accountId 고객 AWS 계정 ID
     * @return Stack 정보
     */
    public StackInfoResponse getStackInfo(Long caseId, String accountId) {
        log.info("Fetching CloudFormation stack info for caseId: {}, accountId: {}", caseId, accountId);

        String stackName = String.format("CIRF-Case-%d-Account-%s", caseId, accountId);
        String roleArn = String.format("arn:aws:iam::%s:role/IRAutomationRole", accountId);

        try {
            // 1. Assume Role로 고객 계정 접근
            CloudFormationClient cfnClient = createCrossAccountCloudFormationClient(roleArn, accountId);

            // 2. Stack 정보 조회
            DescribeStacksRequest request = DescribeStacksRequest.builder()
                    .stackName(stackName)
                    .build();

            DescribeStacksResponse response = cfnClient.describeStacks(request);

            if (response.stacks().isEmpty()) {
                throw new IllegalStateException("Stack이 존재하지 않습니다: " + stackName);
            }

            Stack stack = response.stacks().get(0);

            // 3. Parameters 추출
            Map<String, String> parameters = stack.parameters().stream()
                    .collect(Collectors.toMap(
                            Parameter::parameterKey,
                            Parameter::parameterValue
                    ));

            // 4. Outputs 추출
            Map<String, String> outputs = new HashMap<>();
            if (stack.outputs() != null) {
                outputs = stack.outputs().stream()
                        .collect(Collectors.toMap(
                                Output::outputKey,
                                Output::outputValue
                        ));
            }

            // 5. 시간 포맷
            String createdAt = stack.creationTime() != null
                    ? stack.creationTime().toString()  // ✅ 수정
                    : null;
            String updatedAt = stack.lastUpdatedTime() != null
                    ? stack.lastUpdatedTime().toString()  // ✅ 수정
                    : null;

            log.info("Stack found - Name: {}, Status: {}", stackName, stack.stackStatus());

            return StackInfoResponse.builder()
                    .stackName(stack.stackName())
                    .stackStatus(stack.stackStatusAsString())
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .parameters(parameters)
                    .outputs(outputs)
                    .statusReason(stack.stackStatusReason())
                    .build();

        } catch (CloudFormationException e) {
            if (e.getMessage().contains("does not exist")) {
                log.warn("Stack not found: {}", stackName);
                throw new IllegalStateException("Stack이 아직 생성되지 않았습니다. launchUrl을 통해 먼저 Stack을 생성해주세요.");
            }
            log.error("CloudFormation API error: {}", e.getMessage(), e);
            throw new RuntimeException("Stack 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get stack info: {}", e.getMessage(), e);
            throw new RuntimeException("Stack 정보 조회 실패: " + e.getMessage());
        }
    }

    /**
     * Cross-Account CloudFormation Client 생성
     */
    private CloudFormationClient createCrossAccountCloudFormationClient(String roleArn, String accountId) {
        try {
            // Assume Role
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName("cirf-stack-check-session")
                    .durationSeconds(3600)
                    .build();

            AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            Credentials credentials = assumeRoleResponse.credentials();

            // 임시 자격증명으로 CloudFormation Client 생성
            AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                    credentials.accessKeyId(),
                    credentials.secretAccessKey(),
                    credentials.sessionToken()
            );

            return CloudFormationClient.builder()
                    .region(Region.AP_NORTHEAST_2)
                    .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                    .build();

        } catch (Exception e) {
            log.error("Failed to assume role {}: {}", roleArn, e.getMessage(), e);
            throw new RuntimeException("고객 계정 접근 권한이 없습니다. IRAutomationRole이 생성되었는지 확인하세요.");
        }
    }
}