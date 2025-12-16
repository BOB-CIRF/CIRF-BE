package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.AwsNativeLogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.AwsNativeLogRawDataResponse;
import com.cirf.dashboard.domain.analysis.dto.response.AwsNativeLogResponse;
import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import com.cirf.dashboard.domain.analysis.exception.ErrorMessage;
import com.cirf.dashboard.domain.analysis.exception.LogNotFoundException;
import com.cirf.dashboard.domain.analysis.repository.awsNativeLog.LogEventRepository;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true")
public class AwsNativeLogAnalysisService {

    private final LogEventRepository logEventRepository;
    private final UserRepository userRepository;

    public SliceWithSort<AwsNativeLogResponse> queryLogs(Long userId, AwsNativeLogQueryRequest request) {
        log.info("Querying logs - userId: {}, request: {}", userId, request);

        // 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        String tenantId = String.valueOf(user.getTenant().getId());

        SliceWithSort<LogEvent> eventsWithSort = logEventRepository.queryLogEvents(tenantId, request);

        List<AwsNativeLogResponse> responses = eventsWithSort.content().stream()
                .map(AwsNativeLogResponse::from)
                .toList();

        return SliceWithSort.<AwsNativeLogResponse>builder()
                .content(responses)
                .pageSize(eventsWithSort.pageSize())
                .hasNext(eventsWithSort.hasNext())
                .lastSortValue(eventsWithSort.lastSortValue())
                .totalElements(eventsWithSort.totalElements())
                .build();
    }

    public AwsNativeLogRawDataResponse getRawLogData(Long userId, Long caseId, String logId) {
        log.info("Getting raw log data - userId: {}, caseId: {}, logId: {}", userId, caseId, logId);

        // 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        String tenantId = String.valueOf(user.getTenant().getId());

        // 로그 조회 (라우팅을 위해 caseId 필요)
        LogEvent event = logEventRepository.findByIdWithTenant(tenantId, caseId.toString(), logId)
                .orElseThrow(() -> new LogNotFoundException(ErrorMessage.LOG_NOT_FOUND));

        return AwsNativeLogRawDataResponse.from(event);
    }
}
