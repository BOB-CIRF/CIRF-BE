package com.cirf.dashboard.domain.collect.dto.request;

public record StatusCounts(
        Integer pending,
        Integer process,
        Integer completed,
        Integer fail,
        Integer totalJob
) {
}
