package com.cirf.dashboard.domain.collect.service;

import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.collect.dto.response.ProgressCreateResponse;
import com.cirf.dashboard.domain.collect.entity.CollectStatus;
import com.cirf.dashboard.domain.collect.repository.CollectJobRepository;
import com.cirf.dashboard.domain.collect.repository.CollectStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserRepository userRepository;
    private final CollectJobRepository collectJobRepository;
    private final CollectStatusRepository collectStatusRepository;

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    public ProgressCreateResponse saveCollectProgress(long userId, long caseId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        long tenantId = user.getTenant().getId();

        // progressId 생성 (카운터 기반)
        Long progressId = collectStatusRepository.getNextProgressId();

        // 상태별 카운트 계산
        int completed = 0;
        int pending = 0;
        int process = 0;
        int fail = 0;
        int totalJob = 0;

        String currentTime = UTC_FORMATTER.format(Instant.now());

        // CollectStatus 생성
        CollectStatus collectStatus = CollectStatus.builder()
                .pk(String.format("PROGRESS#%d", progressId))
                .sk(String.format("TENANT#%d#CASE#%d", tenantId, caseId))
                .progressId(progressId)
                .tenantId(tenantId)
                .caseId(caseId)
                .completed(completed)
                .pending(pending)
                .process(process)
                .fail(fail)
                .totalJob(totalJob)
                .updatedAt(currentTime)
                .build();

        // DynamoDB에 저장
        collectStatusRepository.save(collectStatus);

        log.info("CollectStatus saved - progressId: {}, tenantId: {}, caseId: {}, completed: {}, pending: {}, process: {}, fail: {}, totalJob: {}",
                progressId, tenantId, caseId, completed, pending, process, fail, totalJob);

        return new ProgressCreateResponse(progressId);
    }
}
