package com.cirf.dashboard.domain.cases.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3EventService {

    @Value("${aws.region}")
    private String region;

    private final S3Client s3Client;

    public static final String BUCKET_PREFIX = "cirf";

    public String createCaseBucket(Long tenantId, Long caseId) {
        String bucketName = generateBucketName(tenantId, caseId);

        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            CreateBucketResponse response = s3Client.createBucket(createBucketRequest);

            log.info("S3 bucket created successfully: {}", bucketName);
            return bucketName;

        } catch (S3Exception e) {
            log.error("Failed to create S3 bucket: {} - Error: {}",
                    bucketName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 버킷 생성에 실패했습니다: " +
                    e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * 버킷 이름 생성 (S3 네이밍 규칙 준수)
     * 형식: {prefix}-{caseId}-{timestamp}
     */
    private String generateBucketName(Long tenantId, Long caseId) {
        long timestamp = System.currentTimeMillis();
        return String.format("%s-%d-%d-%d", BUCKET_PREFIX, tenantId, caseId, timestamp)
                .toLowerCase();
    }

    /**
     * 버킷 존재 여부 확인
     */
    public boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(builder -> builder.bucket(bucketName));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 버킷 삭제 (사례 삭제 시 사용)
     * 주의: 버킷이 비어있어야 삭제 가능
     */
    public void deleteBucket(String bucketName) {
        try {
            // 실제로는 버킷 내 객체를 먼저 삭제해야 함
            s3Client.deleteBucket(builder -> builder.bucket(bucketName));
            log.info("S3 bucket deleted successfully: {}", bucketName);
        } catch (S3Exception e) {
            log.error("Failed to delete S3 bucket: {} - Error: {}",
                    bucketName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 버킷 삭제에 실패했습니다: " +
                    e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * 버킷 내 모든 객체 삭제 후 버킷 삭제
     */
    public void deleteBucketWithContents(String bucketName) {
        try {
            // 1. 버킷 내 모든 객체 조회 및 삭제
            var listResponse = s3Client.listObjectsV2(builder ->
                    builder.bucket(bucketName)
            );

            if (!listResponse.contents().isEmpty()) {
                var deleteRequest = listResponse.contents().stream()
                        .map(s3Object -> software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder()
                                .key(s3Object.key())
                                .build())
                        .collect(java.util.stream.Collectors.toList());

                s3Client.deleteObjects(builder -> builder
                        .bucket(bucketName)
                        .delete(d -> d.objects(deleteRequest))
                );

                log.info("Deleted {} objects from bucket: {}",
                        deleteRequest.size(), bucketName);
            }

            // 2. 버킷 삭제
            s3Client.deleteBucket(builder -> builder.bucket(bucketName));
            log.info("S3 bucket and contents deleted successfully: {}", bucketName);

        } catch (S3Exception e) {
            log.error("Failed to delete S3 bucket with contents: {} - Error: {}",
                    bucketName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 버킷 및 내용 삭제에 실패했습니다: " +
                    e.awsErrorDetails().errorMessage(), e);
        }
    }
}
