package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.response.LogStashResponse;
import com.cirf.dashboard.domain.analysis.dto.response.PageableDto;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record LogStashListResponse(
        List<LogStashResponse> logs,
        PageableDto pageable
) {
    public static LogStashListResponse of(Page<LogStashResponse> page) {
        return LogStashListResponse.builder()
                .logs(page.getContent())
                .pageable(PageableDto.builder()
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .numberOfElements(page.getNumberOfElements())
                        .totalPages(page.getTotalPages())
                        .totalElements(page.getTotalElements())
                        .isLast(page.isLast())
                        .build())
                .build();
    }

}
