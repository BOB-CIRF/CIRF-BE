package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2FileStatQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2RawFileRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2FileStatResponse;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2RawFileResponse;
import com.cirf.dashboard.domain.analysis.entity.Ec2FileStat;
import com.cirf.dashboard.domain.analysis.repository.ec2File.Ec2FileStatRepository;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.cases.entity.CaseBucket;
import com.cirf.dashboard.domain.cases.exception.ErrorMessage;
import com.cirf.dashboard.domain.cases.exception.NotFoundBucketException;
import com.cirf.dashboard.domain.cases.repository.CaseBucketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class Ec2FileAnalysisService {

    private final UserRepository userRepository;
    private final Ec2FileStatRepository ec2FileStatRepository;
    private final CaseBucketRepository caseBucketRepository;
    private final S3Client s3Client;

    public SliceWithSort<Ec2FileStatResponse> getFileStatList(long userId, Ec2FileStatQueryRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        String tenantId = String.valueOf(user.getTenant().getId());

        // DynamoDB 쿼리 (size + 1로 조회하여 hasNext 확인)
        List<Ec2FileStat> items = ec2FileStatRepository.queryFileStats(
                tenantId,
                request.caseId(),
                request.accountId(),
                request.region(),
                request.instanceId(),
                request.keyword(),
                request.pageNumber(),
                request.pageSize()
        );

        // hasNext 확인 (size + 1로 조회했으므로)
        boolean hasNext = items.size() > request.pageSize();

        // 실제 반환할 데이터는 size만큼만
        List<Ec2FileStat> content = items.stream()
                .limit(request.pageSize())
                .toList();

        // Entity -> DTO 변환
        List<Ec2FileStatResponse> responses = content.stream()
                .map(item -> new Ec2FileStatResponse(
                        item.getInstanceId(),
                        item.getFile(),
                        item.getSize(),
                        item.getAccess(),
                        item.getAccessTime(),
                        item.getModifyTime(),
                        item.getChangeTime()
                ))
                .toList();


        long totalElements = 0;

        totalElements = ec2FileStatRepository.countFileStats(
                    tenantId,
                    request.caseId(),
                    request.accountId(),
                    request.region(),
                    request.instanceId(),
                    request.keyword());
        log.info("Total elements count: {}", totalElements);


        Slice<Ec2FileStatResponse> slice = new SliceImpl<>(
                responses,
                PageRequest.of(request.pageNumber(), request.pageSize()),
                hasNext
        );

        return SliceWithSort.of(slice, null, totalElements);
    }

    public Ec2RawFileResponse getRawFile(long userId, Ec2RawFileRequest request) {
        // 1. User 검증
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // 2. DDB에서 bucketName 조회
        CaseBucket caseBucket = caseBucketRepository.findCaseBucketByUserIdAndCaseId(userId, request.caseId());
        if (caseBucket == null) {
            throw new NotFoundBucketException(ErrorMessage.BUCKET_NOT_FOUND);
        }
        String bucketName = caseBucket.getBucketName();

        // 3. filePath를 파일명으로 변환 (var/log/syslog → var_log_syslog.json.gz)
        String fileName = convertFilePathToFileName(request.filePath());

        // 4. S3에서 객체 목록 조회
        String prefix = String.format("ec2/%s/%s/%s/",
                request.accountId(), request.region(), request.instanceId());

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        // 5. 파일명이 일치하는 객체 필터링
        List<S3Object> matchingObjects = listResponse.contents().stream()
                .filter(s3Object -> s3Object.key().endsWith(fileName))
                .toList();

        if (matchingObjects.isEmpty()) {
            throw new IllegalArgumentException("해당 파일을 찾을 수 없습니다: " + fileName);
        }

        // 6. 타임스탬프가 가장 최신인 객체 찾기
        S3Object latestObject = matchingObjects.stream()
                .max(Comparator.comparing(s3Object -> extractTimestamp(s3Object.key())))
                .orElseThrow(() -> new IllegalArgumentException("타임스탬프를 추출할 수 없습니다."));

        log.info("Latest object found: {}", latestObject.key());

        // 7. S3에서 객체 다운로드 및 압축 해제
        String rawContent = downloadAndDecompressGzipFile(bucketName, latestObject.key());

        return Ec2RawFileResponse.builder()
                .rawContent(rawContent)
                .build();
    }

    /**
     * 파일 경로를 파일명으로 변환
     * 예: var/log/syslog → var_log_syslog.json.gz
     */
    private String convertFilePathToFileName(String filePath) {
        // 슬래시를 언더스코어로 변환
        String converted = filePath.replace("/", "_");
        // .json.gz 확장자 추가
        return converted + ".json.gz";
    }

    /**
     * S3 객체 키에서 타임스탬프 추출
     * 예: ec2/{accountId}/{region}/{instanceId}/{timestamp:20251204T045831Z}/...
     */
    private String extractTimestamp(String key) {
        // 타임스탬프 패턴: 20251204T045831Z 형식
        Pattern pattern = Pattern.compile("/(\\d{8}T\\d{6}Z)/");
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("타임스탬프를 추출할 수 없습니다: " + key);
    }

    /**
     * S3에서 .gz 파일을 다운로드하고 압축 해제하여 문자열로 반환
     */
    private String downloadAndDecompressGzipFile(String bucketName, String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3ObjectInputStream = s3Client.getObject(getObjectRequest);

            // GZIP 압축 해제
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(s3ObjectInputStream);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))) {

                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("Failed to download and decompress file: {}", key, e);
            throw new RuntimeException("파일 다운로드 및 압축 해제에 실패했습니다.", e);
        }
    }
}
