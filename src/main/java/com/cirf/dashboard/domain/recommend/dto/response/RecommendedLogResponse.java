package com.cirf.dashboard.domain.recommend.dto.response;

import lombok.Builder;

@Builder
public record RecommendedLogResponse(
        String logType,
        String displayName
) {
}
