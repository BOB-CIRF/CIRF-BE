package com.cirf.dashboard.domain.collect.dto.request;

public record CollectStatusWebhookRequest(
        Integer progressId,
        String eventType,
        StatusCounts statusCounts,
        String timestamp
) {
}
