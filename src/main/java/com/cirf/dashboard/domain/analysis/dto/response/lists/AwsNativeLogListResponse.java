package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.response.AwsNativeLogResponse;
import com.cirf.dashboard.domain.analysis.dto.response.PageableDto;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record AwsNativeLogListResponse(
        List<AwsNativeLogResponse> logs,
        PageableDto pageable
) {
    public static AwsNativeLogListResponse of(Page<AwsNativeLogResponse> page) {
        return AwsNativeLogListResponse.builder()
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
