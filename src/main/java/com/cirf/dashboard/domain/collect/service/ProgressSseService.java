package com.cirf.dashboard.domain.collect.service;

import com.cirf.dashboard.domain.auth.entity.User;
import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.collect.dto.event.ProgressStatusEvent;
import com.cirf.dashboard.domain.collect.entity.CollectStatus;
import com.cirf.dashboard.domain.collect.exception.ErrorMessage;
import com.cirf.dashboard.domain.collect.exception.ProgressNotFoundException;
import com.cirf.dashboard.domain.collect.exception.UnauthorizedException;
import com.cirf.dashboard.domain.collect.repository.CollectStatusRepository;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressSseService {

    private final CollectStatusRepository collectStatusRepository;
    private final UserRepository userRepository;

    // progressId별 연결된 Emitter들 관리
    // Key: progressId, Value: Set<SseEmitter>
    private final Map<Long, CopyOnWriteArraySet<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // SSE Emitter 타임아웃: 5분 (300초)
    // Heartbeat 30초 간격으로 ALB 60초 타임아웃 방지
    private static final Long SSE_TIMEOUT = 300_000L;


    public void validateUserAccess(long userId, Long progressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // progressId로 CollectStatus 조회
        CollectStatus status = collectStatusRepository.findByProgressId(progressId)
                .orElseThrow(() -> new ProgressNotFoundException(ErrorMessage.PROGRESS_NOT_FOUND));

        // 테넌트 확인
        if (!status.getTenantId().equals(user.getTenant().getId())) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ACCESS);
        }

        log.debug("User access validated - userId: {}, progressId: {}, tenantId: {}",
                userId, progressId, user.getTenant().getId());
    }

    public SseEmitter createEmitter(Long progressId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Emitter 추가
        addEmitter(progressId, emitter);

        // Cleanup 핸들러 등록
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed - progressId: {}", progressId);
            removeEmitter(progressId, emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timeout - progressId: {}", progressId);
            removeEmitter(progressId, emitter);
        });

        emitter.onError((e) -> {
            log.error("SSE connection error - progressId: {}", progressId, e);
            removeEmitter(progressId, emitter);
        });

        log.info("SSE Emitter created - progressId: {}", progressId);
        return emitter;
    }


    private void addEmitter(Long progressId, SseEmitter emitter) {
        emitters.computeIfAbsent(progressId, k -> new CopyOnWriteArraySet<>())
                .add(emitter);
        log.info("SSE Emitter added - progressId: {}, total connections: {}",
                progressId, emitters.get(progressId).size());
    }

    private void removeEmitter(Long progressId, SseEmitter emitter) {
        Set<SseEmitter> progressEmitters = emitters.get(progressId);
        if (progressEmitters != null) {
            progressEmitters.remove(emitter);
            if (progressEmitters.isEmpty()) {
                emitters.remove(progressId);
                log.info("All SSE connections closed for progressId: {}", progressId);
            } else {
                log.info("SSE Emitter removed - progressId: {}, remaining connections: {}",
                        progressId, progressEmitters.size());
            }
        }
    }

    public void sendCurrentStatus(Long progressId, SseEmitter emitter) {
        try {
            Optional<CollectStatus> statusOpt = collectStatusRepository.findByProgressId(progressId);

            if (statusOpt.isPresent()) {
                CollectStatus status = statusOpt.get();
                ProgressStatusEvent event = ProgressStatusEvent.from(status);

                // 1. progress 이벤트 전송 (ApiResponse로 감싸기)
                ApiResponse<ProgressStatusEvent> response = new ApiResponse<>(
                        HttpStatus.OK.value(),
                        "진행 상황 조회 성공",
                        event
                );

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(response));

                log.info("Initial status sent - progressId: {}, totalJob: {}, completed: {}/{}, terminal: {}",
                        progressId, event.getTotalJob(), event.getCompleted(), event.getTotalJob(), event.getTerminal());

                // 2. 이미 완료된 작업이면 complete 이벤트 전송 후 즉시 연결 종료
                if (event.isTerminal()) {
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data("Collection already completed"));

                    emitter.complete();
                    removeEmitter(progressId, emitter);

                    log.info("Collection already completed, SSE connection closed immediately - progressId: {}", progressId);
                }
            } else {
                // 초기 상태 (아직 작업이 시작되지 않음)
                ProgressStatusEvent initialEvent = ProgressStatusEvent.builder()
                        .progressId(progressId)
                        .completed(0)
                        .pending(0)
                        .process(0)
                        .fail(0)
                        .totalJob(0)
                        .terminal(false)
                        .updatedAt(null)
                        .build();

                ApiResponse<ProgressStatusEvent> response = new ApiResponse<>(
                        HttpStatus.OK.value(),
                        "진행 상황 조회 성공",
                        initialEvent
                );

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(response));

                log.info("Initial empty status sent - progressId: {}", progressId);
            }
        } catch (IOException e) {
            log.error("Failed to send initial status - progressId: {}", progressId, e);
            removeEmitter(progressId, emitter);
        }
    }

    public void broadcastProgress(Long progressId, ProgressStatusEvent event) {
        Set<SseEmitter> progressEmitters = emitters.get(progressId);

        if (progressEmitters == null || progressEmitters.isEmpty()) {
            log.debug("No active SSE connections for progressId: {}", progressId);
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : progressEmitters) {
            try {
                // ApiResponse로 감싸서 전송
                ApiResponse<ProgressStatusEvent> response = new ApiResponse<>(
                        HttpStatus.OK.value(),
                        "진행 상황 업데이트",
                        event
                );

                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(response));

                log.debug("Progress event sent - progressId: {}, completed: {}/{}, terminal: {}",
                        progressId, event.getCompleted(), event.getTotalJob(), event.getTerminal());

                // 완료 시 연결 종료
                if (event.isTerminal()) {
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data("Collection completed"));
                    emitter.complete();
                    deadEmitters.add(emitter);
                    log.info("Collection completed, closing SSE connection - progressId: {}", progressId);
                }
            } catch (IOException e) {
                log.error("Failed to send SSE event - progressId: {}", progressId, e);
                deadEmitters.add(emitter);
            }
        }

        // 실패한 Emitter 제거
        deadEmitters.forEach(emitter -> removeEmitter(progressId, emitter));

        log.info("Broadcast complete - progressId: {}, sent: {}, failed: {}",
                progressId, progressEmitters.size() - deadEmitters.size(), deadEmitters.size());
    }

    @Scheduled(fixedDelay = 30000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        log.debug("Sending heartbeat to {} active progress streams", emitters.size());

        emitters.forEach((progressId, emitterSet) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitterSet) {
                try {
                    // 빈 코멘트 전송 (연결 유지용)
                    emitter.send(SseEmitter.event()
                            .comment("heartbeat"));

                    log.trace("Heartbeat sent - progressId: {}", progressId);
                } catch (IOException e) {
                    log.warn("Heartbeat failed - progressId: {}", progressId);
                    deadEmitters.add(emitter);
                }
            }

            deadEmitters.forEach(emitter -> removeEmitter(progressId, emitter));
        });
    }

    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProgressIds", emitters.size());
        stats.put("totalConnections", emitters.values().stream()
                .mapToInt(Set::size)
                .sum());
        stats.put("details", new HashMap<>(emitters.entrySet().stream()
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey().toString(), entry.getValue().size()),
                        HashMap::putAll)));

        return stats;
    }
}
