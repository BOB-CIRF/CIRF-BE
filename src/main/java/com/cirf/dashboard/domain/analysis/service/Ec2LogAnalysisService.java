package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2LogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2CollectResponse;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogDetailResponse;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogResponse;
import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import com.cirf.dashboard.domain.analysis.exception.ErrorMessage;
import com.cirf.dashboard.domain.analysis.exception.LogNotFoundException;
import com.cirf.dashboard.domain.analysis.repository.ec2Log.Ec2LogEventRepository;
import com.cirf.dashboard.domain.analysis.repository.ec2File.Ec2FileStatRepository;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanEc2Response;
import com.cirf.dashboard.domain.scan.service.ScanEc2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Ec2LogAnalysisService {

    private final UserRepository userRepository;
    private final Ec2LogEventRepository ec2LogEventRepository;
    private final Ec2FileStatRepository ec2FileStatRepository;
    private final ScanEc2Service scanEc2Service;

    public SliceWithSort<Ec2LogResponse> queryEc2Log(
            long userId,
            Ec2LogQueryRequest queryRequest
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        String tenantId = String.valueOf(user.getTenant().getId());

        // searchByQuery 사용 (동적 조건 + 라우팅 적용)
        SliceWithSort<Ec2LogEvent> eventsWithSort = ec2LogEventRepository.queryEc2LogEvents(tenantId, queryRequest);

        // Entity를 DTO로 변환
        List<Ec2LogResponse> responses = eventsWithSort.content().stream()
                .map(Ec2LogResponse::from)
                .toList();

        return SliceWithSort.<Ec2LogResponse>builder()
                .content(responses)
                .pageSize(eventsWithSort.pageSize())
                .hasNext(eventsWithSort.hasNext())
                .lastSortValue(eventsWithSort.lastSortValue())
                .totalElements(eventsWithSort.totalElements())
                .build();
    }

    public Ec2LogDetailResponse getEc2LogDetail(long userId, Long caseId, String logId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        String tenantId = String.valueOf(user.getTenant().getId());

        Ec2LogEvent event = ec2LogEventRepository.findByIdWithRouting(tenantId, caseId, logId)
                .orElseThrow(() -> new LogNotFoundException(ErrorMessage.LOG_NOT_FOUND));

        return Ec2LogDetailResponse.from(event);
    }

    public Page<Ec2CollectResponse> getEc2Collects(
            long userId,
            long caseId,
            String accountId,
            String region,
            int page,
            int size
    ) {
        log.info("Getting EC2 collects - userId: {}, caseId: {}, accountId: {}, region: {}, page: {}, size: {}",
                userId, caseId, accountId, region, page, size);

        // 1. User 조회 및 tenantId 가져오기
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        String tenantId = String.valueOf(user.getTenant().getId());

        // 2. ScanEc2Service를 통해 스캔된 EC2 목록 가져오기
        // accountId와 region을 그대로 전달 (null이면 전체 조회)
        ScanResultsRequest scanRequest = new ScanResultsRequest(
                caseId,
                accountId != null ? accountId : "*",  // null이면 모든 accountId
                region,                                // null이면 모든 region
                0,                                     // 페이징 없이 전체 조회
                Integer.MAX_VALUE
        );

        Slice<ScanEc2Response> scanResults = scanEc2Service.getEc2Lists(userId, scanRequest);
        List<ScanEc2Response> allEc2s = scanResults.getContent();

        String accountInfo = accountId != null ? "accountId: " + accountId : "all accounts";
        String regionInfo = region != null ? "region: " + region : "all regions";
        log.info("Found {} scanned EC2 instances ({}, {})", allEc2s.size(), accountInfo, regionInfo);

        // 3. Ec2FileStat이 존재하는 EC2만 필터링 (병렬 처리)
        List<Ec2CollectResponse> filteredEc2s = allEc2s.parallelStream()
                .filter(ec2 -> {
                    boolean exists = ec2FileStatRepository.existsByInstance(
                            tenantId,
                            caseId,
                            ec2.accountId(),
                            ec2.region(),
                            ec2.instanceId()
                    );
                    if (exists) {
                        log.debug("EC2 has file stats - instanceId: {}, region: {}",
                                ec2.instanceId(), ec2.region());
                    }
                    return exists;
                })
                .map(ec2 -> new Ec2CollectResponse(
                        ec2.instanceId(),
                        ec2.instanceName(),
                        ec2.instanceType(),
                        ec2.region(),
                        ec2.accountId()
                ))
                .toList();

        log.info("Filtered to {} EC2 instances with file stats", filteredEc2s.size());

        // 4. 페이징 처리
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + size, filteredEc2s.size());

        List<Ec2CollectResponse> pagedContent;
        if (start >= filteredEc2s.size()) {
            pagedContent = List.of();
        } else {
            pagedContent = filteredEc2s.subList(start, end);
        }

        return new PageImpl<>(pagedContent, pageable, filteredEc2s.size());
    }
}
