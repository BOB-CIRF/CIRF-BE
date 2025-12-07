package com.cirf.dashboard.domain.collect.service;

import com.cirf.dashboard.domain.collect.dto.event.ProgressStatusEvent;
import com.cirf.dashboard.domain.collect.dto.request.CollectStatusWebhookRequest;
import com.cirf.dashboard.domain.collect.dto.request.StatusCounts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectStatusService {

    private final ProgressSseService progressSseService;


    public void updateStatus(CollectStatusWebhookRequest request) {
        Long progressId = (long) request.progressId();
        StatusCounts counts = request.statusCounts();

        log.info("Updating collect status - progressId: {}, eventType: {}, completed: {}/{}",
                progressId, request.eventType(), counts.completed(), counts.totalJob());

        ProgressStatusEvent event = ProgressStatusEvent.builder()
                .progressId(progressId)
                .completed(counts.completed())
                .pending(counts.pending())
                .process(counts.process())
                .fail(counts.fail())
                .totalJob(counts.totalJob())
                .terminal(isJobCompleted(counts))
                .updatedAt(request.timestamp())
                .build();

        progressSseService.broadcastProgress(progressId, event);

        log.info("Status update complete - progressId: {}, terminal: {}", progressId, event.getTerminal());
    }

    private boolean isJobCompleted(StatusCounts counts) {
        return counts.totalJob() > 0 && (counts.completed() + counts.fail()) >= counts.totalJob();
    }
}
