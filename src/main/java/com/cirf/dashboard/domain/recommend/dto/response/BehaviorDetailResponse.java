package com.cirf.dashboard.domain.recommend.dto.response;

import com.cirf.dashboard.domain.recommend.entity.CollectRecommendBehavior;
import lombok.Builder;

import java.util.List;

@Builder
public record BehaviorDetailResponse(
        String behaviorId,
        String categoryId,
        String title,
        List<LogMappingResponse> logs
) {
    public static BehaviorDetailResponse from(CollectRecommendBehavior behavior) {
        List<LogMappingResponse> logResponses = behavior.getLogs() != null
                ? behavior.getLogs().stream()
                .map(LogMappingResponse::from)
                .toList()
                : List.of();

        return BehaviorDetailResponse.builder()
                .behaviorId(behavior.getBehaviorId())
                .categoryId(behavior.getCategoryId())
                .title(behavior.getTitle())
                .logs(logResponses)
                .build();
    }
}
