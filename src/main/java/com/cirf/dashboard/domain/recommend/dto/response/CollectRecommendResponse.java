package com.cirf.dashboard.domain.recommend.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CollectRecommendResponse(
        List<RecommendedLogResponse> recommendedLogs
) {
}
