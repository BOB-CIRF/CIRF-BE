package com.cirf.dashboard.domain.analysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Ec2RawFileRequest(
        @NotNull(message = "caseId는 필수입니다.")
        Long caseId,
        @NotBlank(message = "accountId는 필수입니다.")
        String accountId,
        @NotBlank(message = "region은 필수입니다.")
        String region,
        @NotBlank(message = "instanceId는 필수입니다.")
        String instanceId,
        @NotBlank(message = "fileName 필수입니다.")
        String fileName
) {
}
