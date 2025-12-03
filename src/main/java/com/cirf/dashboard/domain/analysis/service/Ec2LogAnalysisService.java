package com.cirf.dashboard.domain.analysis.service;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2LogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogDetailResponse;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogResponse;
import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import com.cirf.dashboard.domain.analysis.exception.ErrorMessage;
import com.cirf.dashboard.domain.analysis.exception.LogNotFoundException;
import com.cirf.dashboard.domain.analysis.repository.ec2Log.Ec2LogEventRepository;
import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Ec2LogAnalysisService {

    private final UserRepository userRepository;
    private final Ec2LogEventRepository ec2LogEventRepository;

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
}
