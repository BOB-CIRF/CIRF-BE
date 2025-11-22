package com.cirf.dashboard.domain.collect.controller;

import com.cirf.dashboard.domain.collect.dto.response.JobDetailResponse;
import com.cirf.dashboard.domain.collect.dto.response.JobListResponse;
import com.cirf.dashboard.domain.collect.service.CollectJobService;
import com.cirf.dashboard.global.common.dto.ApiResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class JobsController {

    private final CollectJobService collectJobService;

    @GetMapping("/{collectId}/jobs")
    public ApiResponse<JobListResponse> getJobs(
            @RequestHeader("userId") @NotNull Long userId,
            @PathVariable Integer collectId,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "20") int pageSize
    ) {
        log.info("Get jobs request - collectId: {}, page: {}, size: {}", collectId, pageNumber, pageSize);

        Slice<JobDetailResponse> response = collectJobService.getJobDetail(userId, collectId, pageNumber, pageSize);

        return new ApiResponse<>(HttpStatus.OK.value(), ResponseMessage.GET_COLLECT_LIST_SUCCESS.getMessage(), JobListResponse.of(response));
    }
}
