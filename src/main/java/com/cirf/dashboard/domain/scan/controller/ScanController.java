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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScanController {

    private static final int DEFAULT_PAGE_SIZE = 9;

    private final ScanLogsService scanLogsService;
    private final ScanEc2Service scanEc2Service;

    @GetMapping("/logs/{caseId}")
    public ApiResponse<ScanResultsListResponse> getScanResults(
            @RequestHeader("tenant_id") long tenantId,
            @PathVariable long caseId,
            @Valid ScanResultsRequest request
    ) {
        log.info("Received scan results request - tenantId: {}, caseId: {}, request: {}", tenantId, caseId, request);

        Slice<ScanResultsResponse> scanResults = scanLogsService.getScanResultsByRegion(tenantId, caseId, request);

        log.info("Scan results retrieved successfully - count: {}", scanResults.getNumberOfElements());

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_SCAN_RESULTS_SUCCESS.getMessage(),
                ScanResultsListResponse.of(scanResults)
        );
    }

    @PostMapping("/ec2")
    public ApiResponse<ScanCompletedResponse> ec2ScanRequest(
            @RequestHeader("tenant_id") Long tenantId,
            @RequestBody @Valid ScanEc2Request scanEc2Request
            ){

        ScanCompletedResponse response = scanEc2Service.scanEc2Request(tenantId, scanEc2Request.caseId(), scanEc2Request.accountId());

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.EC2_SCAN_SUCCESS.getMessage(),
                response
        );
    }

    @GetMapping("/ec2/{scanEc2Id}")
    public ApiResponse<ScanEc2ListResponse> getEc2ScanResults(
            @RequestHeader("tenant_id") Long tenantId,
            @PathVariable Long scanEc2Id,
            @Valid ScanResultsRequest request
    ){
        Slice<ScanEc2Response> results = scanEc2Service.getEc2Lists(tenantId, scanEc2Id, request);
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_EC2_RESULTS_SUCCESS.getMessage(),
                ScanEc2ListResponse.of(results)
        );
    }
}
