package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.cases.repository.AccountIdRepository;
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

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanEc2Service {

    private final ScanEc2Repository scanEc2Repository;
    private final UserRepository userRepository;
    private final AccountIdRepository accountIdRepository;
    private final AsyncScanEc2Service asyncScanEc2Service;

    public ScanCompletedResponse scanEc2Request(long userId, long caseId, String accountId) {
        // userId 검증
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // 1. ScanEc2Metadata 생성
        ScanEc2Metadata metadata = scanEc2Repository.createScanEc2Metadata(user, caseId, accountId)
                .orElseThrow(() -> new ScanEc2MetadataCreationException(ErrorMessage.FAILED_CREATE_EC2_METADATA));

        Long ec2ScanId = metadata.getScanId();
        log.info("Created EC2 scan with ID: {}", ec2ScanId);

        // 2. 비동기 호출 (리전 조회도 비동기 내부에서 수행)
        log.info("Calling async scan on thread: {}", Thread.currentThread().getName());
        asyncScanEc2Service.saveScanEc2Instances(ec2ScanId, accountId);
        log.info("Async scan triggered, returning response immediately");

        return new ScanCompletedResponse(ec2ScanId);
    }

    public Slice<ScanEc2Response> getEc2Lists(long userId, ScanResultsRequest request) {
        log.info("getEc2Lists - userId: {}, caseId: {}, accountId: {}, region: {}",
                userId, request.caseId(), request.accountId(), request.region());

        // 0) 사용자 검증
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        // 1) region 검증 (옵션)
        final String region = request.region();
        if (region != null && !region.isEmpty()) {
            if (!AwsRegion.isValidRegion(region)) {
                throw new InvalidRegionException(ErrorMessage.INVALID_REGION);
            }
        }

        // 2) 조회 대상 accountId 목록 확정
        final List<String> targetAccountIds;
        if (request.accountId() == null || request.accountId().isEmpty()
                || "*".equals(request.accountId()) || "ALL".equalsIgnoreCase(request.accountId())) {
            targetAccountIds = accountIdRepository.findAccountIdsByIncidentCaseId(request.caseId());
            if (targetAccountIds == null || targetAccountIds.isEmpty()) {
                log.info("No accounts registered for caseId: {}, returning empty result", request.caseId());
                Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
                return new SliceImpl<>(Collections.emptyList(), pageable, false);
            }
        } else {
            targetAccountIds = List.of(request.accountId());
        }
        log.info("Target accounts: {} (caseId: {})", targetAccountIds.size(), request.caseId());

        // 3) accountId별 최신 EC2 스캔 메타데이터 → scanId 수집 (병렬) + scanId -> accountId 매핑 생성
        Map<Long, String> scanIdToAccountMap = new HashMap<>();
        for (String accountId : targetAccountIds) {
            scanEc2Repository.findLatestEc2Scan(userId, request.caseId(), accountId)
                    .ifPresent(metadata -> scanIdToAccountMap.put(metadata.getScanId(), accountId));
        }

        if (scanIdToAccountMap.isEmpty()) {
            log.info("No EC2 scan metadata found for caseId: {}, returning empty result", request.caseId());
            Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        List<Long> ec2ScanIds = new ArrayList<>(scanIdToAccountMap.keySet());
        log.info("Collected {} latest EC2 scanIds: {}", ec2ScanIds.size(), ec2ScanIds);

        // 4) 스캔ID들로 EC2 인스턴스 일괄 조회 (region 필터 적용)
        List<EnabledInstances> instances;

        // 권장: 레포/서비스에 배치 메서드가 있을 때
        instances = scanEc2Repository.getEc2InstancesByRegionIn(ec2ScanIds, region);

        // 만약 위 메서드가 없다면 fallback (for-loop로 병렬/순차 조회)
        // instances = ec2ScanIds.parallelStream()
        //         .flatMap(scanId -> scanEc2Repository.getEc2InstancesByRegion(scanId, region).stream())
        //         .toList();

        String regionInfo = (region == null || region.isEmpty()) ? "all regions" : "region: " + region;
        log.info("Found {} instances from {} scan(s), {}", instances.size(), ec2ScanIds.size(), regionInfo);

        // 5) DTO 변환 (필요 시 정렬 추가 가능)
        List<ScanEc2Response> ec2Responses = instances.stream()
                .map(instance -> new ScanEc2Response(
                        instance.getIdxId(),
                        scanIdToAccountMap.get(instance.getEc2ScanId()),  // scanId로 accountId 매핑
                        instance.getInstanceId(),
                        instance.getInstanceName(),
                        instance.getInstanceType(),
                        instance.getRegion(),
                        instance.getStatus(),
                        instance.getPlatformDetails(),
                        instance.getPublicIp()
                ))
                // .sorted(Comparator.comparing(ScanEc2Response::region).thenComparing(ScanEc2Response::instanceName))
                .toList();

        // 6) 안전한 페이징
        Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
        int start = (int) pageable.getOffset();
        if (start >= ec2Responses.size()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }
        int end = Math.min(start + pageable.getPageSize(), ec2Responses.size());
        List<ScanEc2Response> pagedContent = ec2Responses.subList(start, end);
        boolean hasNext = end < ec2Responses.size();

        return new SliceImpl<>(pagedContent, pageable, hasNext);
    }

}
