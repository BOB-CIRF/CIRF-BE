package com.cirf.dashboard.domain.analysis.controller;

import com.cirf.dashboard.domain.analysis.dto.request.LogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.LogRawDataResponse;
import com.cirf.dashboard.domain.analysis.dto.response.LogStashResponse;
import com.cirf.dashboard.domain.analysis.dto.response.lists.LogStashListResponse;
import com.cirf.dashboard.domain.analysis.service.AnalysisService;
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
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/logs")
    public ApiResponse<LogStashListResponse> queryLogs(
            @RequestHeader("userId") @NotNull Long userId,
            @Valid LogQueryRequest request
    ) {
        Page<LogStashResponse> logs = analysisService.queryLogs(userId, request);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_LOGS_SUCCESS.getMessage(),
                LogStashListResponse.of(logs)
        );
    }

    @GetMapping("/logs/{id}/raw")
    public ApiResponse<LogRawDataResponse> getRawLogData(
            @RequestHeader("userId") @NotNull Long userId,
            @PathVariable String id,
            @RequestParam Long caseId
    ) {
        LogRawDataResponse rawData = analysisService.getRawLogData(userId, caseId, id);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_RAW_LOG_SUCCESS.getMessage(),
                rawData
        );
    }
}
