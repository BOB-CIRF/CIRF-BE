package com.cirf.dashboard.domain.analysis.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 제외
public record PageableDto(
        Integer pageNumber,        // search_after 방식에서는 사용 안함
        int pageSize,
        Integer numberOfElements,
        Integer totalPages,        // search_after 방식에서는 사용 안함
        Long totalElements,        // 첫 페이지만 포함, 이후는 null
        boolean isLast,
        String nextSearchAfter     // 예: "1733220000000_sort_id_123"
) {}
