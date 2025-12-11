package com.cirf.dashboard.domain.recommend.dto.response;

import com.cirf.dashboard.domain.recommend.entity.CollectRecommendBehavior;
import lombok.Builder;

@Builder
public record BehaviorSummaryResponse(
        String behaviorId,
        String categoryId,
        String title
) {
    public static BehaviorSummaryResponse from(CollectRecommendBehavior behavior) {
        return BehaviorSummaryResponse.builder()
                .behaviorId(behavior.getBehaviorId())
                .categoryId(behavior.getCategoryId())
                .title(behavior.getTitle())
                .build();
    }
}
