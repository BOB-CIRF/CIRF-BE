package com.cirf.dashboard.domain.analysis.dto.response;

import lombok.Builder;

@Builder
public record PageableDto(
        int pageNumber,
        int pageSize,
        int numberOfElements,
        int totalPages,
        long totalElements,
        boolean isLast
) {}
