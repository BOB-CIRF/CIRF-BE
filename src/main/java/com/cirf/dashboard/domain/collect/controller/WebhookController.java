package com.cirf.dashboard.domain.collect.controller;

import com.cirf.dashboard.domain.collect.dto.ProgressEvent;
import com.cirf.dashboard.domain.collect.dto.request.CollectStatusWebhookRequest;
import com.cirf.dashboard.domain.collect.dto.request.WebhookRequest;
import com.cirf.dashboard.domain.collect.service.CollectStatusService;
import com.cirf.dashboard.domain.collect.service.ProgressCacheService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ProgressCacheService progressCacheService;
    private final CollectStatusService collectStatusService;

    @PostMapping("/progress")
    public ApiResponse<String> post(
            @RequestBody WebhookRequest request
    ) {
        log.info("Webhook received: {}", request.toString());

        // 캐시에 저장하고 대기 중인 요청들에게 알림
        progressCacheService.updateProgress(ProgressEvent.toEntity(request));

        return new ApiResponse<>(HttpStatus.OK.value(), "success", null);
    }

    @PostMapping("/collect-status")
    public ApiResponse<String> postCollectStatus(
            @RequestBody CollectStatusWebhookRequest request
    ){
        log.info("Collect status webhook received: progressId={}, eventType={}, statusCounts={}",
                request.progressId(), request.eventType(), request.statusCounts());

        // CollectStatus 업데이트 및 SSE 브로드캐스트
        collectStatusService.updateStatus(request);

        return new ApiResponse<>(HttpStatus.OK.value(), "success", null);
    }
}
