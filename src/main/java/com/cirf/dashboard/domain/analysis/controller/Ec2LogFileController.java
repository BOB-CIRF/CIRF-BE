package com.cirf.dashboard.domain.analysis.controller;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.request.Ec2FileStatQueryRequest;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2FileStatResponse;
import com.cirf.dashboard.domain.analysis.dto.response.lists.Ec2FileStatListResponse;
import com.cirf.dashboard.domain.analysis.service.Ec2FileAnalysisService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis/ec2")
@RequiredArgsConstructor
public class Ec2LogFileController {

    private final Ec2FileAnalysisService ec2FileAnalysisService;

    @GetMapping("/no-timestamp")
    public ApiResponse<Ec2FileStatListResponse> getFileStat(
            @RequestHeader("userId") long userId,
            @Valid Ec2FileStatQueryRequest request
    ){
        SliceWithSort<Ec2FileStatResponse> response = ec2FileAnalysisService.getFileStatList(userId, request);
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                ResponseMessage.GET_LOGS_SUCCESS.getMessage(),
                Ec2FileStatListResponse.of(response)
        );
    }
}
