package com.cirf.dashboard.domain.scan.controller;

import com.cirf.dashboard.domain.scan.dto.request.ScanEc2Request;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
import com.cirf.dashboard.domain.scan.dto.response.ScanCompletedResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanEc2Response;
import com.cirf.dashboard.domain.scan.dto.response.lists.ScanEc2ListResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanResultsResponse;
import com.cirf.dashboard.domain.scan.dto.response.lists.ScanResultsListResponse;
import com.cirf.dashboard.domain.scan.service.ScanEc2Service;
import com.cirf.dashboard.domain.scan.service.ScanLogsService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanLogsService scanLogsService;
    private final ScanEc2Service scanEc2Service;

    @GetMapping("/logs")
    public ApiResponse<ScanResultsListResponse> getScanResults(
            @RequestHeader("userId") @NotNull long userId,
            @Valid ScanResultsRequest request
    ) {
        log.info("Received scan results request - userId: {}, caseId: {}, request: {}", userId, request.caseId(), request);

        Slice<ScanResultsResponse> scanResults = scanLogsService.getScanResultsByRegion(userId, request.caseId(), request);

        log.info("Scan results retrieved successfully - count: {}", scanResults.getNumberOfElements());

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_SCAN_RESULTS_SUCCESS.getMessage(),
                ScanResultsListResponse.of(scanResults)
        );
    }

    @PostMapping("/ec2")
    public ApiResponse<ScanCompletedResponse> ec2ScanRequest(
            @RequestHeader("userId") @NotNull Long userId,
            @RequestBody @Valid ScanEc2Request scanEc2Request
            ){
        ScanCompletedResponse response = scanEc2Service.scanEc2Request(userId, scanEc2Request.caseId(), scanEc2Request.accountId());

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.EC2_SCAN_SUCCESS.getMessage(),
                response
        );
    }

    @GetMapping("/ec2")
    public ApiResponse<ScanEc2ListResponse> getEc2ScanResults(
            @RequestHeader("userId") @NotNull Long userId,
            @Valid ScanResultsRequest request
    ){
        Slice<ScanEc2Response> results = scanEc2Service.getEc2Lists(userId, request);
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_EC2_RESULTS_SUCCESS.getMessage(),
                ScanEc2ListResponse.of(results)
        );
    }
}
