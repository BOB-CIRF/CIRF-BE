package com.cirf.dashboard.domain.collect.service;

import com.cirf.dashboard.domain.auth.exception.UserNotFoundException;
import com.cirf.dashboard.domain.auth.repository.UserRepository;
import com.cirf.dashboard.domain.collect.dto.ProgressEvent;
import com.cirf.dashboard.domain.collect.dto.response.ProgressResponse;
import com.cirf.dashboard.domain.collect.entity.CollectProgress;
import com.cirf.dashboard.domain.collect.repository.CollectProgressRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressCacheService {

    private final Cache<Integer, ProgressEvent> progressEventCache;
    private final CollectProgressRepository progressRepository;
    private final UserRepository userRepository;

    // collectId별 대기 중인 요청들을 관리
    // Key: collectId, Value: Callback 맵 (requestId -> Callback)
    private final Map<Integer, ConcurrentHashMap<String, java.util.function.Consumer<ProgressResponse>>> waitingRequests =
            new ConcurrentHashMap<>();

    /**
     * 유저 검증
     * @param userId 유저 ID
     * @throws UserNotFoundException 유저를 찾을 수 없는 경우
     */
    public void validateUser(long userId) {
        log.info("Validating user - userId: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("User not found with id: {}", userId);
            throw new UserNotFoundException();
        }

        log.info("User validated successfully - userId: {}", userId);
    }

    /**
     * 현재 진행 상황을 즉시 조회 (캐시 또는 DB)
     * revision이 더 새롭거나 terminal이면 반환, 아니면 Empty
     */
    public Optional<ProgressResponse> getCurrentProgress(Integer collectId, String currentRevision) {
        log.info("Getting current progress - collectId: {}, currentRevision: {}", collectId, currentRevision);

        // 1. 캐시에서 현재 이벤트 확인
        ProgressEvent cachedEvent = progressEventCache.getIfPresent(collectId);

        // 2. 캐시에 이벤트가 있고 revision이 다르면 즉시 반환
        if (cachedEvent != null && isRevisionNewer(cachedEvent.getRevision(), currentRevision)) {
            log.info("Found updated event in cache - collectId: {}, newRevision: {}",
                    collectId, cachedEvent.getRevision());
            return Optional.of(ProgressResponse.from(cachedEvent));
        }

        // 3. 캐시에 없으면 DB 조회
        if (cachedEvent == null) {
            log.info("Cache miss, checking DB - collectId: {}", collectId);
            Optional<ProgressEvent> dbEvent = loadProgressFromDb(collectId);

            if (dbEvent.isPresent()) {
                ProgressEvent event = dbEvent.get();
                // revision이 더 새롭거나, terminal이면 즉시 반환
                if (isRevisionNewer(event.getRevision(), currentRevision) || event.isTerminal()) {
                    log.info("Found event in DB - collectId: {}, revision: {}, terminal: {}",
                            collectId, event.getRevision(), event.isTerminal());
                    return Optional.of(ProgressResponse.from(event));
                }
                cachedEvent = event;
            }
        }

        // 4. 캐시에는 있지만 terminal=true이면 즉시 반환 (더 이상 변경 없음)
        if (cachedEvent != null && cachedEvent.isTerminal()) {
            log.info("Job already completed - collectId: {}", collectId);
            return Optional.of(ProgressResponse.from(cachedEvent));
        }

        // 5. 새로운 이벤트가 없음
        return Optional.empty();
    }

    public Runnable waitForProgressUpdate(Integer collectId, java.util.function.Consumer<ProgressResponse> callback) {
        log.info("Waiting for progress update - collectId: {}", collectId);

        String requestId = java.util.UUID.randomUUID().toString();

        // 대기 목록에 추가
        waitingRequests.computeIfAbsent(collectId, k -> new ConcurrentHashMap<>())
                .put(requestId, callback);

        // 취소 핸들러 반환
        return () -> {
            ConcurrentHashMap<String, java.util.function.Consumer<ProgressResponse>> waiting = waitingRequests.get(collectId);
            if (waiting != null) {
                waiting.remove(requestId);
                if (waiting.isEmpty()) {
                    waitingRequests.remove(collectId);
                }
            }
        };
    }

    public void updateProgress(ProgressEvent event) {
        Integer collectId = event.getCollectId();
        log.info("Updating progress for collectId: {}, revision: {}, terminal: {}",
                collectId, event.getRevision(), event.isTerminal());

        // 1. 임시로 캐시에 저장 (대기 중인 요청들을 위해)
        progressEventCache.put(collectId, event);

        // 2. 대기 중인 요청들에게 알림
        ConcurrentHashMap<String, java.util.function.Consumer<ProgressResponse>> waiting = waitingRequests.get(collectId);
        if (waiting != null && !waiting.isEmpty()) {
            log.info("Notifying {} waiting requests for collectId: {}", waiting.size(), collectId);

            ProgressResponse response = ProgressResponse.from(event);

            waiting.values().forEach(callback -> callback.accept(response));
            waiting.clear();
        }

        // 3. 작업이 완료되면 캐시에서 삭제 (이후 조회는 DB에서)
        if (event.isTerminal()) {
            log.info("Job completed, removing from cache - collectId: {}", collectId);
            progressEventCache.invalidate(collectId);
        }
    }


    private Optional<ProgressEvent> loadProgressFromDb(Integer collectId) {
        try {
            Optional<CollectProgress> progressOpt = progressRepository.findLatestByCollectId(collectId);

            if (progressOpt.isEmpty()) {
                log.info("No progress found in DynamoDB - collectId: {}", collectId);
                return Optional.empty();
            }

            CollectProgress progress = progressOpt.get();

            // CollectProgress를 ProgressEvent로 변환
            ProgressEvent event = ProgressEvent.builder()
                    .revision(String.valueOf(progress.getRevision()))
                    .jobId(progress.getJobId())
                    .collectId(progress.getCollectId())
                    .completed(progress.getCompleted())
                    .pending(progress.getPending())
                    .process(progress.getProcess())
                    .fail(progress.getFail())
                    .totalJob(progress.getTotalJob())
                    .terminal(progress.getTerminal())
                    .updatedAt(progress.getUpdatedAt())
                    .build();

            // 캐시에도 저장 (다음 요청을 위해)
            progressEventCache.put(collectId, event);
            log.info("Loaded progress from DB and cached - collectId: {}, revision: {}",
                    collectId, progress.getRevision());

            return Optional.of(event);
        } catch (Exception e) {
            log.error("Failed to load progress from DynamoDB - collectId: {}", collectId, e);
            return Optional.empty();
        }
    }

    private boolean isRevisionNewer(String newRevision, String currentRevision) {
        try {
            int newRev = Integer.parseInt(newRevision);
            int currRev = Integer.parseInt(currentRevision);
            return newRev > currRev;
        } catch (NumberFormatException e) {
            // 숫자로 파싱할 수 없는 경우 문자열로 비교
            log.warn("Failed to parse revision as integer, comparing as strings - new: {}, current: {}",
                    newRevision, currentRevision);
            return !newRevision.equals(currentRevision);
        }
    }
}
