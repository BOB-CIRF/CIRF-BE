package com.cirf.dashboard.domain.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@EnableScheduling
public class DeploymentSseHub {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private String key(Long caseId, String accountId) {
        return caseId + ":" + accountId;
    }

    public SseEmitter subscribe(Long caseId, String accountId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        String k = key(caseId, accountId);

        emitters.computeIfAbsent(k, __ -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(k, emitter));
        emitter.onTimeout(() -> {
            try { emitter.complete(); } catch (Exception ignored) {}
            remove(k, emitter);
        });
        emitter.onError(e -> remove(k, emitter));

        safeSend(emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    public void publish(Long caseId, String accountId, Object payload) {
        String k = key(caseId, accountId);
        List<SseEmitter> list = emitters.get(k);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            if (!safeSend(emitter, SseEmitter.event().name("deployment-status").data(payload))) {
                remove(k, emitter);
            }
        }
    }

    public void complete(Long caseId, String accountId, Object payload) {
        String k = key(caseId, accountId);
        List<SseEmitter> list = emitters.get(k);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            safeSend(emitter, SseEmitter.event().name("complete").data(payload));
            try { emitter.complete(); } catch (Exception ignored) {}
            remove(k, emitter);
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        int streamCount = emitters.size();
        log.debug("Sending heartbeat to {} deployment streams", streamCount);

        // key별로 순회
        emitters.forEach((k, list) -> {
            if (list == null || list.isEmpty()) return;

            for (SseEmitter emitter : list) {
                boolean ok = safeSend(emitter, SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("ts", System.currentTimeMillis())));

                if (!ok) {
                    remove(k, emitter);
                }
            }
        });
    }

    private void remove(String k, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(k);
        if (list == null) return;

        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(k);
    }

    private boolean safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }
}


