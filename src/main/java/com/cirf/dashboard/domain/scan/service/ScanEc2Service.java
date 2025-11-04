package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanCompletedResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanEc2Response;
import com.cirf.dashboard.domain.scan.entity.EnabledInstances;
import com.cirf.dashboard.domain.scan.entity.ScanEc2Metadata;
import com.cirf.dashboard.domain.scan.exception.*;
import com.cirf.dashboard.domain.scan.repository.ScanEc2Repository;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.scan.service.enums.AwsRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanEc2Service {

    private final ScanEc2Repository scanEc2Repository;
    private final UserRepository userRepository;
    private final AsyncScanEc2Service asyncScanEc2Service;

    public ScanCompletedResponse scanEc2Request(long userId, long caseId, String accountId) {
        // userId 검증
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        // 1. ScanEc2Metadata 생성
        ScanEc2Metadata metadata = scanEc2Repository.createScanEc2Metadata(userId, caseId, accountId)
                .orElseThrow(() -> new ScanEc2MetadataCreationException(ErrorMessage.FAILED_CREATE_EC2_METADATA));

        Long ec2ScanId = metadata.getScanId();
        log.info("Created EC2 scan with ID: {}", ec2ScanId);

        // 2. 비동기 호출 (리전 조회도 비동기 내부에서 수행)
        log.info("Calling async scan on thread: {}", Thread.currentThread().getName());
        asyncScanEc2Service.saveScanEc2Instances(ec2ScanId, accountId);
        log.info("Async scan triggered, returning response immediately");

        return new ScanCompletedResponse(ec2ScanId);
    }

    public Slice<ScanEc2Response> getEc2Lists(long userId, ScanResultsRequest request){
        log.info("getEc2Lists - userId: {}, caseId: {}, accountId: {}, region: {}",
                userId, request.caseId(), request.accountId(), request.region());

        // userId 검증
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        // userId, caseId, accountId로 가장 최근의 EC2 Scan 메타데이터를 가져오기
        ScanEc2Metadata ec2Metadata = scanEc2Repository.findLatestEc2Scan(userId, request.caseId(), request.accountId())
                .orElseThrow(() -> new NotFoundEc2MetadataException(ErrorMessage.EC2_METADATA_NOT_FOUND));

        Long ec2ScanId = ec2Metadata.getScanId();
        log.info("Found latest EC2 scan - ec2ScanId: {}", ec2ScanId);

        // region 검증 (region이 지정된 경우에만)
        if (request.region() != null && !request.region().isEmpty()) {
            if (!AwsRegion.isValidRegion(request.region())) {
                throw new InvalidRegionException(ErrorMessage.INVALID_REGION);
            }
        }

        // region별 EC2 인스턴스 조회 (region이 null/empty면 모든 리전 조회)
        List<EnabledInstances> instances = scanEc2Repository.getEc2InstancesByRegion(ec2ScanId, request.region());

        String regionInfo = (request.region() == null || request.region().isEmpty())
                ? "all regions"
                : "region: " + request.region();
        log.info("Found {} instances for ec2ScanId: {}, {}", instances.size(), ec2ScanId, regionInfo);

        // EnabledInstances를 ScanEc2Response로 변환
        List<ScanEc2Response> ec2Responses = instances.stream()
                .map(instance -> new ScanEc2Response(
                        instance.getIdxId(),
                        instance.getInstanceId(),
                        instance.getInstanceName(),
                        instance.getInstanceType(),
                        instance.getRegion(),
                        instance.getStatus(),
                        instance.getPlatformDetails(),
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
