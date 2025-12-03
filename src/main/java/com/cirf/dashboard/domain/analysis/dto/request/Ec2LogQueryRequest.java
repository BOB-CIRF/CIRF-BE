package com.cirf.dashboard.domain.analysis.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record Ec2LogQueryRequest(
        @NotNull(message = "caseId는 필수입니다.")
        long caseId,
        @NotBlank(message = "instanceId는 필수입니다.")
        String instanceId,
        @NotBlank(message = "accountId는 필수입니다.")
        String accountId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String logType,
        String fileName,
        String activity,
        String outcome,
        String keyword,
        @NotNull(message = "pageNumber는 필수입니다.")
        @Min(value = 0, message = "pageNumber는 0 이상이어야 합니다.")
        Integer pageNumber,
        @NotNull(message = "pageSize는 필수입니다.")
        @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
        Integer pageSize
) {
}
