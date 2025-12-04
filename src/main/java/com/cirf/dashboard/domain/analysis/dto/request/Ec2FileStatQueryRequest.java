package com.cirf.dashboard.domain.analysis.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record Ec2FileStatQueryRequest(
        @NotNull(message = "caseId는 필수입니다.")
        Long caseId,
        @NotBlank(message = "accountId는 필수입니다.")
        String accountId,
        @NotBlank(message = "region은 필수입니다.")
        String region,
        @NotBlank(message = "instanceId는 필수입니다.")
        String instanceId,
        String keyword,
        LocalDateTime startTime,
        LocalDateTime endTime,
        @NotNull(message = "pageNumber는 필수입니다.")
        @Min(value = 0, message = "pageNumber는 0 이상이어야 합니다.")
        Integer pageNumber,
        @NotNull(message = "pageSize는 필수입니다.")
        @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
        Integer pageSize
) {
    public Ec2FileStatQueryRequest {
        if (pageNumber == null || pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
    }
}
