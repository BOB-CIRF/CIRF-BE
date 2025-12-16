package com.cirf.dashboard.domain.analysis.dto.request;

import jakarta.validation.constraints.NotNull;

public record Ec2CollectRequest(
        @NotNull(message = "caseId는 필수입니다.") Long caseId,
        String accountId,
        String region
) {
}
