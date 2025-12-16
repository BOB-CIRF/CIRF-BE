package com.cirf.dashboard.domain.analysis.controller;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2CollectRequest;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2LogQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2CollectResponse;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogDetailResponse;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogResponse;
import com.cirf.dashboard.domain.analysis.dto.response.lists.Ec2CollectListResponse;
import com.cirf.dashboard.domain.analysis.dto.response.lists.Ec2LogListResponse;
import com.cirf.dashboard.domain.analysis.service.Ec2LogAnalysisService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis/ec2")
@RequiredArgsConstructor
public class Ec2LogAnalysisController {

    private final Ec2LogAnalysisService ec2LogAnalysisService;

    @GetMapping
    public ApiResponse<Ec2CollectListResponse> getCollectEc2List(
            @RequestHeader("userId") @NotNull Long userId,
            @Valid @ModelAttribute Ec2CollectRequest request,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "20") int pageSize
    ){
        Page<Ec2CollectResponse> response = ec2LogAnalysisService.getEc2Collects(
                userId, request.caseId(), request.accountId(), request.region(), pageNumber, pageSize);

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_EC2_LIST.getMessage(),
                Ec2CollectListResponse.of(response)
        );
    }

    @GetMapping("/timestamp")
    public ApiResponse<Ec2LogListResponse> getEc2TimestampList(
            @RequestHeader("userId") long userId,
            @Valid Ec2LogQueryRequest request
    ){
        SliceWithSort<Ec2LogResponse> response = ec2LogAnalysisService.queryEc2Log(userId, request);
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_LOGS_SUCCESS.getMessage(),
                Ec2LogListResponse.of(response)
        );
    }

    @GetMapping("/timestamp/{ec2LogId}/raw")
    public ApiResponse<Ec2LogDetailResponse> getEc2RawData(
            @RequestHeader("userId") long userId,
            @RequestParam long caseId,
            @PathVariable String ec2LogId
    ){
        Ec2LogDetailResponse response = ec2LogAnalysisService.getEc2LogDetail(userId, caseId, ec2LogId);
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_RAW_LOG_SUCCESS.getMessage(),
                response
        );
    }



}
