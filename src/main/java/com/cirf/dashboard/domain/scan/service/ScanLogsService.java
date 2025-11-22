package com.cirf.dashboard.domain.scan.service;

import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanResultsResponse;
import com.cirf.dashboard.domain.scan.entity.ScanLogsMetadata;
import com.cirf.dashboard.domain.scan.exception.ErrorMessage;
import com.cirf.dashboard.domain.scan.exception.RegionNotFoundException;
import com.cirf.dashboard.domain.scan.exception.ScanNotCompletedException;
import com.cirf.dashboard.domain.scan.exception.ScanNotFoundException;
import com.cirf.dashboard.domain.scan.repository.ScanLogsRepository;
import com.cirf.dashboard.domain.scan.service.enums.LogType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanLogsService {

    private final ScanLogsRepository scanLogsRepository;
    private final UserRepository userRepository;

    public Slice<ScanResultsResponse> getScanResultsByRegion(long userId, long caseId, ScanResultsRequest request) {
        log.info("getScanResultsByRegion - userId: {}, caseId: {}, accountId: {}, region: {}",
                userId, caseId, request.accountId(), request.region());

        // userId 검증
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        // userId, caseId, accountId로 가장 최근의 Scan 메타데이터를 가져오기
        ScanLogsMetadata latestScan = scanLogsRepository.findLatestScan(userId, caseId, request.accountId())
                .orElseThrow(() -> new ScanNotFoundException(ErrorMessage.SCAN_NOT_FOUND));

        log.info("Found latestScan - scanId: {}, status: {}", latestScan.getScanId(), latestScan.getStatus());

        // 스캔 status가 완료가 아니라면 예외 처리
        if (!"SUCCEEDED".equals(latestScan.getStatus())) {
            log.warn("Scan not completed - status: {}", latestScan.getStatus());
            throw new ScanNotCompletedException(ErrorMessage.SCAN_NOT_COMPLETED);
        }

        // region이 지정된 경우에만 region 검증
        if (request.region() != null) {
            String scanPk = "SCAN#" + latestScan.getScanId();
            String regionSk = "REG#" + request.region();

            log.debug("Checking region status - scanPk: {}, regionSk: {}", scanPk, regionSk);

            scanLogsRepository.findScanRegionStatus(scanPk, regionSk)
                    .orElseThrow(() -> new RegionNotFoundException(ErrorMessage.REGION_NOT_FOUND));

            log.info("Region found, checking enabled logs");
        } else {
            log.info("No specific region requested, checking all regions");
        }

        Pageable pageable = PageRequest.of(request.pageNumber(), request.pageSize());
        return checkEnabledLogsForRegion(latestScan.getScanId(), request.accountId(), request.region(), pageable);
    }

    private Slice<ScanResultsResponse> checkEnabledLogsForRegion(Long scanId, String accountId, String region, Pageable pageable) {
        // 모든 로그 타입에 대한 결과 생성
        List<ScanResultsResponse> allResults = Arrays.stream(LogType.values())
                .map(logType -> new ScanResultsResponse(
                        logType.getType(),
                        scanLogsRepository.existsEnabledLog(scanId, accountId, region, logType.getType())
                ))
                .toList();

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allResults.size());

        // 시작 위치가 전체 크기보다 크면 빈 리스트 반환
        if (start >= allResults.size()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        List<ScanResultsResponse> pageContent = allResults.subList(start, end);
        boolean hasNext = end < allResults.size();

        return new SliceImpl<>(pageContent, pageable, hasNext);
    }
}
