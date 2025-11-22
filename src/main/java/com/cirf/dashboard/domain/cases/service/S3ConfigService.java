package com.cirf.dashboard.domain.cases.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ConfigService {

    private final S3Client s3Client;

    /**
     * S3 버킷에 SQS 이벤트 알림 설정
     * S3에 파일 업로드 시 자동으로 SQS에 메시지 전송
     */
    public void setupS3ToSqsNotification(String bucketName, String queueArn, String queueUrl) {
        try {
            // 1. SQS 큐 정책 설정 (S3가 메시지를 보낼 수 있도록 허용)
            setQueuePolicyForS3(queueUrl, queueArn, bucketName);

            // 2. S3 이벤트 알림 설정
            configureS3EventNotification(bucketName, queueArn);

            log.info("S3 to SQS notification configured successfully for bucket: {} -> queue: {}",
                    bucketName, queueArn);

        } catch (Exception e) {
            log.error("Failed to setup S3 to SQS notification: {}", e.getMessage(), e);
            throw new RuntimeException("S3-SQS 알림 설정에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 버킷 이름 검증
     * SQS 큐 정책이 이미 와일드카드(cirf-*-*-*)로 설정되어 있으므로 정책 업데이트 불필요
     */
    private void setQueuePolicyForS3(String queueUrl, String queueArn, String bucketName) {
        try {
            // 버킷 이름이 cirf-로 시작하는지 검증
            if (!bucketName.startsWith("cirf-")) {
                throw new IllegalArgumentException("버킷 이름은 'cirf-'로 시작해야 합니다: " + bucketName);
            }

            log.info("Bucket '{}' is covered by wildcard SQS policy (cirf-*-*-*). No policy update needed.", bucketName);
        } catch (Exception e) {
            log.error("Failed to validate bucket name: {}", e.getMessage());
            throw new RuntimeException("버킷 이름 검증 실패: " + e.getMessage(), e);
        }
    }


    /**
     * S3 이벤트 알림 설정
     */
    private void configureS3EventNotification(String bucketName, String queueArn) {
        try {
            // S3 이벤트 알림 설정
            NotificationConfiguration notificationConfiguration = NotificationConfiguration.builder()
                    .queueConfigurations(
                            QueueConfiguration.builder()
                                    .queueArn(queueArn)
                                    .events(
                                            Event.S3_OBJECT_CREATED_PUT,
                                            Event.S3_OBJECT_CREATED_POST,
                                            Event.S3_OBJECT_CREATED_COPY
                                    )
                                    .build()
                    )
                    .build();

            PutBucketNotificationConfigurationRequest request =
                    PutBucketNotificationConfigurationRequest.builder()
                            .bucket(bucketName)
                            .notificationConfiguration(notificationConfiguration)
                            .build();

            s3Client.putBucketNotificationConfiguration(request);
            log.info("S3 event notification configured for bucket: {}", bucketName);

        } catch (Exception e) {
            log.error("Failed to configure S3 event notification: {}", e.getMessage());
            throw new RuntimeException("S3 이벤트 알림 설정 실패", e);
        }
    }
}