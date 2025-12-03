package com.cirf.dashboard.domain.analysis.controller;

import com.cirf.dashboard.domain.analysis.dto.request.AwsNativeLogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.AwsNativeLogRawDataResponse;
import com.cirf.dashboard.domain.analysis.dto.response.AwsNativeLogResponse;
import com.cirf.dashboard.domain.analysis.dto.response.lists.AwsNativeLogListResponse;
import com.cirf.dashboard.domain.analysis.service.AwsNativeLogAnalysisService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true")
public class AwsNativeLogAnalysisController {

    private final AwsNativeLogAnalysisService awsNativeLogAnalysisService;

    @GetMapping("/logs")
    public ApiResponse<AwsNativeLogListResponse> queryLogs(
            @RequestHeader("userId") @NotNull Long userId,
            @Valid AwsNativeLogQueryRequest request
    ) {
        Page<AwsNativeLogResponse> logs = awsNativeLogAnalysisService.queryLogs(userId, request);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_LOGS_SUCCESS.getMessage(),
                AwsNativeLogListResponse.of(logs)
        );
    }

    @GetMapping("/logs/{id}/raw")
    public ApiResponse<AwsNativeLogRawDataResponse> getRawLogData(
            @RequestHeader("userId") @NotNull Long userId,
            @PathVariable String id,
            @RequestParam Long caseId
    ) {
        AwsNativeLogRawDataResponse rawData = awsNativeLogAnalysisService.getRawLogData(userId, caseId, id);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_RAW_LOG_SUCCESS.getMessage(),
                rawData
        );
    }
}
