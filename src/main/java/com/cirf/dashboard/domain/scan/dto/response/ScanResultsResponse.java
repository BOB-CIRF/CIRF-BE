package com.cirf.dashboard.domain.scan.dto.response;

public record ScanResultsResponse(
        String logType,
        Boolean enabled
) {
}
