package com.cirf.dashboard.domain.cases.dto.response;

import lombok.Builder;

@Builder
public record NotCreatedResponse(
        long caseId,
        String accountId,
        String stackName,
        String stackStatus,
        String statusReason
) {
}
