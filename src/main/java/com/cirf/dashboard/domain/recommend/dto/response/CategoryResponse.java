package com.cirf.dashboard.domain.recommend.dto.response;

import com.cirf.dashboard.domain.recommend.entity.CollectRecommendCategory;
import lombok.Builder;

@Builder
public record CategoryResponse(
        String categoryId,
        String name
) {
    public static CategoryResponse from(CollectRecommendCategory category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .build();
    }
}
