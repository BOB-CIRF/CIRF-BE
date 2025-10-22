package com.cirf.dashboard.domain.scan.controller;

import com.cirf.dashboard.domain.scan.dto.request.ScanEc2Request;
import com.cirf.dashboard.domain.scan.dto.request.ScanResultsRequest;
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
    public ApiResponse<Long> ec2ScanResults(
            @RequestHeader("tenant_id") Long id,
            @RequestBody @Valid ScanEc2Request scanEc2Request
            ){

        Long scanEc2Id = scanEc2Service.scanEc2Request(id, scanEc2Request.caseId(), scanEc2Request.accountId());

        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.EC2_SCAN_SUCCESS.getMessage(),
                scanEc2Id
        );
    }
}
