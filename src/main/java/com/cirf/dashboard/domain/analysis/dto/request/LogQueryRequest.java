package com.cirf.dashboard.domain.analysis.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for querying LogStash logs from ElasticSearch
 */
public record LogQueryRequest(
        @NotNull(message = "caseId는 필수입니다.")
        Long caseId,

        String accountId,

        String region,

        String logType,

        String activity,

        String outcome,

        LocalDateTime startTime,

        LocalDateTime endTime,

        @NotNull(message = "pageNumber는 필수입니다.")
        @Min(value = 0, message = "pageNumber는 0 이상이어야 합니다.")
        Integer pageNumber,

        @NotNull(message = "pageSize는 필수입니다.")
        @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
        Integer pageSize
) {
}
