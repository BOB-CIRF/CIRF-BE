package com.cirf.dashboard.domain.collect.dto.request;

public record WebhookJob(
        String logType,
        String dest,
        String status,
        String startDate,
        String endDate
) {
}
