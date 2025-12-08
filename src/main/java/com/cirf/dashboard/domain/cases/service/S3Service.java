package com.cirf.dashboard.domain.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
/**
 * 프로덕션 환경용 S3 서비스
 * IAM Role 기반 인증 사용 (Access Key 불필요)
 */
@Slf4j
@Service
public class S3Service {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket-prefix:cirf-case}")
    private String bucketPrefix;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        // DefaultCredentialsProvider가 다음 순서로 자격증명 찾음:
        // 1. 환경변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
        // 2. 시스템 프로퍼티
        // 3. ~/.aws/credentials 파일
        // 4. EC2 Instance Profile (IAM Role) ⭐ 프로덕션에서 주로 사용
        // 5. ECS Container credentials

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("S3Client initialized with DefaultCredentialsProvider for region: {}", region);
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3Client closed");
        }
    }

    /**
     * 사례용 S3 버킷 생성
     * 버킷 이름 형식: {bucket-prefix}-{caseId}-{timestamp}
     */
    public String createCaseBucket(Long caseId, String caseName) {
        String bucketName = generateBucketName(caseId);

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
    private String generateBucketName(Long caseId) {
        long timestamp = System.currentTimeMillis();
        return String.format("%s-%d-%d", bucketPrefix, caseId, timestamp)
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-");
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
    // S3Service.java (아래 메서드만 추가)
    public S3Client getS3Client() {
        return s3Client;
    }
}
