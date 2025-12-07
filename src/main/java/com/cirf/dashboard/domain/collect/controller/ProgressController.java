package com.cirf.dashboard.domain.collect.controller;

import com.cirf.dashboard.domain.collect.dto.response.ProgressCreateResponse;
import com.cirf.dashboard.domain.collect.dto.response.ProgressResponse;
import com.cirf.dashboard.domain.collect.service.ProgressCacheService;
import com.cirf.dashboard.domain.collect.service.ProgressService;
import com.cirf.dashboard.domain.collect.service.ProgressSseService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressCacheService progressCacheService;
    private final ProgressService progressService;
    private final ProgressSseService progressSseService;

    @PostMapping("/progress")
    public ApiResponse<ProgressCreateResponse> saveCollectProgressStatus(
            @RequestHeader("userId") long userId,
            @RequestParam("caseId") long caseId
    ){
        ProgressCreateResponse progressId = progressService.saveCollectProgress(userId, caseId);
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.SAVE_COLLECT_PROGRESS_SUCCESS.getMessage(),
                progressId
        );
    }

    @GetMapping("/{collectId}/progress")
    public DeferredResult<ApiResponse<ProgressResponse>> getProgress(
            @RequestHeader("userId") long userId,
            @PathVariable Integer collectId,
            @RequestParam(required = false, defaultValue = "0") String revision,
            @RequestParam(required = false, defaultValue = "30") long timeout
    ) {
        log.info("Long polling request - userId: {}, collectId: {}, revision: {}, timeout: {}",
                userId, collectId, revision, timeout);

        // 유저 검증 - 예외 발생 시 전역 핸들러가 처리
        progressCacheService.validateUser(userId);

        // 타임아웃은 최대 60초로 제한 (밀리초로 변환)
        long effectiveTimeout = Math.min(timeout, 60) * 1000L;

        DeferredResult<ApiResponse<ProgressResponse>> deferredResult = new DeferredResult<>(effectiveTimeout);

        // 타임아웃 시 204 No Content 반환 (정상 응답)
        deferredResult.onTimeout(() -> {
            log.info("Long polling timeout - collectId: {}", collectId);
            deferredResult.setResult(
                    new ApiResponse<>(HttpStatus.NO_CONTENT.value(), "No updates available", null)
            );
        });

        // 1. 현재 진행 상황 즉시 조회
        Optional<ProgressResponse> currentProgress = progressCacheService.getCurrentProgress(collectId, revision);

        if (currentProgress.isPresent()) {
            // 즉시 반환
            log.info("Found current progress, returning immediately - collectId: {}", collectId);
            deferredResult.setResult(
                    new ApiResponse<>(HttpStatus.OK.value(),
                            ResponseMessage.GET_COLLECT_PROCESS_SUCCESS.getMessage(),
                            currentProgress.get())
            );
        } else {
            // 변경 대기
            log.info("Waiting for progress update - collectId: {}", collectId);
            Runnable cancelHandler = progressCacheService.waitForProgressUpdate(collectId, response -> {
                if (!deferredResult.isSetOrExpired()) {
                    deferredResult.setResult(
                            new ApiResponse<>(HttpStatus.OK.value(),
                                    ResponseMessage.GET_COLLECT_PROCESS_SUCCESS.getMessage(),
                                    response)
                    );
                }
            });

            // DeferredResult 완료 시 대기 취소
            deferredResult.onCompletion(() -> {
                log.debug("Long polling completed - collectId: {}", collectId);
                cancelHandler.run();
            });
        }

        return deferredResult;
    }


    @GetMapping(value = "/progress/{progressId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(
            @RequestHeader("userId") long userId,
            @PathVariable Long progressId
    ) {
        log.info("SSE stream request - userId: {}, progressId: {}", userId, progressId);

        // 1. 유저 권한 검증
        progressSseService.validateUserAccess(userId, progressId);

        // 2. SSE Emitter 생성 (타임아웃: 5분)
        SseEmitter emitter = progressSseService.createEmitter(progressId);

        // 3. 현재 상태 즉시 전송
        progressSseService.sendCurrentStatus(progressId, emitter);

        log.info("SSE stream established - userId: {}, progressId: {}", userId, progressId);

        return emitter;
    }
}
