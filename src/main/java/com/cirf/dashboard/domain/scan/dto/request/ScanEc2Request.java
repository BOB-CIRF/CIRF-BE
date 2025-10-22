package com.cirf.dashboard.domain.scan.dto.request;

public record ScanEc2Request(
        Long caseId,
        String accountId
) {
}
