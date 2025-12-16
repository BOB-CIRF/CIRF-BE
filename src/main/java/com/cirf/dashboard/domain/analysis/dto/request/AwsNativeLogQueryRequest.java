package com.cirf.dashboard.domain.analysis.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for querying LogStash logs from ElasticSearch
 */
public record AwsNativeLogQueryRequest(
        @NotNull(message = "caseId는 필수입니다.")
        Long caseId,

        String accountId,

        String region,

        String logType,

        String activity,

        String outcome,

        LocalDateTime startTime,

        LocalDateTime endTime,

        // 키워드 검색 (multi_match로 여러 필드 검색)
        String keyword,

        @NotNull(message = "pageSize는 필수입니다.")
        @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
        Integer pageSize,
        String searchAfter  // 예: "1733220000000_sort_id_123"
) {
}
