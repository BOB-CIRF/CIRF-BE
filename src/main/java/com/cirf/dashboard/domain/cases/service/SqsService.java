package com.cirf.dashboard.domain.cases.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AWS SQS 서비스
 * 다수의 S3 버킷이 하나의 공유 SQS 큐를 구독하도록 관리
 */

// 변경점 요약:
// 1) 공유 SQS 큐 생성: createSharedStandardQueueForS3() - 케이스별이 아닌 전체 시스템용
// 2) 다수 버킷 지원: addMultipleS3SendMessagePolicy() - 여러 버킷이 하나의 큐에 메시지 전송
// 3) 동적 버킷 추가: addBucketToExistingPolicy() - 기존 큐에 새 버킷 권한 추가
// 4) 정책 빌더: buildMultipleS3SendMessagePolicy() - 배열 형태의 버킷 ARN 지원

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.sqs.queue-prefix}")
    private String queuePrefix;

    private final SqsClient sqsClient;

    /**
     * (신규) S3 알림용 공유 Standard 큐 생성 (다수의 S3 Event → 하나의 SQS)
     * ex) cirf-case-s3-events-shared
     */
    public String createSharedStandardQueueForS3() {
        String queueName = generateSharedQueueName();
        try {
            Map<QueueAttributeName, String> attributes = new HashMap<>();
            // Standard 큐: FIFO 관련 속성 넣지 않는다
            attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, "345600"); // 4일
            attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "300");          // 5분
            attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20"); // long polling

            CreateQueueResponse response = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(queueName)
                            .attributes(attributes)
                            .build()
            );
            log.info("Shared standard queue for S3 created: {} ({})", queueName, response.queueUrl());
            return response.queueUrl();
        } catch (QueueNameExistsException e) {
            log.warn("Queue already exists: {}", queueName);
            return getQueueUrl(queueName);
        } catch (SqsException e) {
            throw new RuntimeException("Shared Standard SQS 큐 생성 실패: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * 다수의 S3 버킷이 SendMessage 할 수 있도록 큐 정책 설정
     * @param queueUrl SQS 큐 URL
     * @param queueArn SQS 큐 ARN
     * @param bucketNames 허용할 S3 버킷 이름 목록
     * @param sourceAccount AWS 계정 ID (선택사항)
     */
    public void addMultipleS3SendMessagePolicy(String queueUrl, String queueArn,
                                               java.util.List<String> bucketNames, String sourceAccount) {
        String policy = buildMultipleS3SendMessagePolicy(queueArn, bucketNames, sourceAccount);

        Map<QueueAttributeName, String> attrs = new HashMap<>();
        attrs.put(QueueAttributeName.POLICY, policy);

        sqsClient.setQueueAttributes(
                SetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributes(attrs)
                        .build()
        );

        log.info("Attached multiple S3 SendMessage policy to queue: {} for buckets: {}", queueUrl, bucketNames);
    }

    /**
     * 기존 큐에 새로운 S3 버킷 권한 추가
     * 현재 정책을 읽어서 새 버킷을 추가하는 방식
     * @param queueUrl SQS 큐 URL
     * @param newBucketName 추가할 S3 버킷 이름
     * @param sourceAccount AWS 계정 ID (선택사항)
     */
    public void addBucketToExistingPolicy(String queueUrl, String newBucketName, String sourceAccount) {
        try {
            // 현재 정책 가져오기
            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.POLICY, QueueAttributeName.QUEUE_ARN)
                            .build()
            );

            String queueArn = response.attributes().get(QueueAttributeName.QUEUE_ARN);
            String currentPolicy = response.attributes().get(QueueAttributeName.POLICY);

            List<String> bucketNames = new ArrayList<>();

            // 기존 정책에서 버킷 ARN 목록 파싱
            if (currentPolicy != null && !currentPolicy.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode policyJson = mapper.readTree(currentPolicy);
                    JsonNode statements = policyJson.get("Statement");

                    if (statements != null && statements.isArray()) {
                        for (JsonNode statement : statements) {
                            JsonNode condition = statement.get("Condition");
                            if (condition != null) {
                                JsonNode arnLike = condition.get("ArnLike");
                                if (arnLike != null) {
                                    JsonNode sourceArn = arnLike.get("aws:SourceArn");
                                    if (sourceArn != null) {
                                        if (sourceArn.isArray()) {
                                            for (JsonNode arn : sourceArn) {
                                                String arnStr = arn.asText();
                                                // arn:aws:s3:::bucket-name 형식에서 버킷 이름 추출
                                                String bucketName = arnStr.replace("arn:aws:s3:::", "");
                                                bucketNames.add(bucketName);
                                            }
                                        } else {
                                            String arnStr = sourceArn.asText();
                                            String bucketName = arnStr.replace("arn:aws:s3:::", "");
                                            bucketNames.add(bucketName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse existing policy, will create new policy: {}", e.getMessage());
                }
            }

            // 새 버킷이 이미 목록에 있는지 확인
            if (!bucketNames.contains(newBucketName)) {
                bucketNames.add(newBucketName);
                log.info("Adding new bucket '{}' to policy. Total buckets: {}", newBucketName, bucketNames.size());
            } else {
                log.info("Bucket '{}' already exists in policy", newBucketName);
                return;
            }

            // 정책 재생성 및 적용
            String updatedPolicy = buildMultipleS3SendMessagePolicy(queueArn, bucketNames, sourceAccount);
            Map<QueueAttributeName, String> attrs = new HashMap<>();
            attrs.put(QueueAttributeName.POLICY, updatedPolicy);

            sqsClient.setQueueAttributes(
                    SetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributes(attrs)
                            .build()
            );

            log.info("Added bucket '{}' to existing queue policy: {}. Total buckets in policy: {}",
                    newBucketName, queueUrl, bucketNames.size());
        } catch (SqsException e) {
            log.error("Failed to add bucket to policy: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("버킷 권한 추가 실패", e);
        }
    }

    /**
     * S3가 SendMessage 할 수 있도록 큐 정책 추가 (단일 버킷용 - 하위 호환성)
     * 내부적으로 addMultipleS3SendMessagePolicy 호출
     */
    public void addS3SendMessagePolicy(String queueUrl, String queueArn, String bucketName, String sourceAccount) {
        addMultipleS3SendMessagePolicy(queueUrl, queueArn, java.util.List.of(bucketName), sourceAccount);
    }

    /**
     * 케이스 큐 생성 (이제 공유 큐 사용)
     * 케이스별로 개별 큐를 만들지 않고 전체 시스템에서 하나의 공유 큐 사용
     */
    public String createCaseQueue(Long caseId, String caseName) {
        return createSharedStandardQueueForS3();
    }

    /**
     * 다수의 S3 버킷을 위한 정책 생성
     * aws:SourceArn 조건에 배열 형태로 여러 버킷 ARN 지정
     */
    private String buildMultipleS3SendMessagePolicy(String queueArn, java.util.List<String> bucketNames, String sourceAccount) {
        // 버킷 ARN 목록 생성 (JSON 배열 형식)
        String bucketArns = bucketNames.stream()
                .map(bucket -> String.format("\"arn:aws:s3:::%s\"", bucket))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        if (sourceAccount == null || sourceAccount.isBlank()) {
            return String.format("""
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": { "Service": "s3.amazonaws.com" },
                  "Action": "SQS:SendMessage",
                  "Resource": "%s",
                  "Condition": {
                    "ArnLike": { "aws:SourceArn": [%s] }
                  }
                }
              ]
            }""", queueArn, bucketArns);
        } else {
            return String.format("""
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": { "Service": "s3.amazonaws.com" },
                  "Action": "SQS:SendMessage",
                  "Resource": "%s",
                  "Condition": {
                    "ArnLike": { "aws:SourceArn": [%s] },
                    "StringEquals": { "aws:SourceAccount": "%s" }
                  }
                }
              ]
            }""", queueArn, bucketArns, sourceAccount);
        }
    }

    /**
     * 단일 버킷용 정책 생성 (하위 호환성)
     */
    private String buildS3SendMessagePolicy(String queueArn, String bucketName, String sourceAccount) {
        return buildMultipleS3SendMessagePolicy(queueArn, java.util.List.of(bucketName), sourceAccount);
    }

    private String generateFifoQueueName(Long caseId) {
        return String.format("%s-%d.fifo", queuePrefix, caseId)
                .toLowerCase()
                .replaceAll("[^a-z0-9-_.]", "-");
    }

    private String generateStandardQueueName(Long caseId) {
        return String.format("%s-%d", queuePrefix, caseId)
                .toLowerCase()
                .replaceAll("[^a-z0-9-_.]", "-");
    }

    /**
     * 공유 큐 이름 생성
     * ex) cirf-case-s3-events-shared
     */
    private String generateSharedQueueName() {
        return String.format("%s-s3-events-shared", queuePrefix)
                .toLowerCase()
                .replaceAll("[^a-z0-9-_.]", "-");
    }

    public String getQueueUrl(String queueName) {
        try {
            return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
        } catch (QueueDoesNotExistException e) {
            log.warn("Queue does not exist: {}", queueName);
            return null;
        }
    }

    public String getQueueArn(String queueUrl) {
        try {
            GetQueueAttributesResponse resp = sqsClient.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build());
            return resp.attributes().get(QueueAttributeName.QUEUE_ARN);
        } catch (SqsException e) {
            log.error("Failed to get queue ARN: {}", e.awsErrorDetails().errorMessage());
            return null;
        }
    }

    public void deleteQueue(String queueUrl) {
        try {
            sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
            log.info("SQS queue deleted: {}", queueUrl);
        } catch (SqsException e) {
            throw new RuntimeException("SQS 큐 삭제 실패: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public boolean queueExists(String queueName) {
        return getQueueUrl(queueName) != null;
    }

    public void sendTestMessage(String queueUrl, String message) {
        try {
            SendMessageResponse resp = sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(message)
                            // Standard 큐는 messageGroupId 불필요
                            .build()
            );
            log.info("Test message sent. MessageId={}", resp.messageId());
        } catch (SqsException e) {
            log.error("Failed to send message: {}", e.awsErrorDetails().errorMessage());
        }
    }
}