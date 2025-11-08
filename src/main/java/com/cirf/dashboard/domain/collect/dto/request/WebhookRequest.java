package com.cirf.dashboard.domain.collect.dto.request;

public record WebhookRequest(
        String revision,
        String jobId,
        Integer collectId,
        Integer completed,
        Integer pending,
        Integer process,
        Integer fail,
        Integer totalJob,
        boolean terminal,
        String updatedAt
) {
}

