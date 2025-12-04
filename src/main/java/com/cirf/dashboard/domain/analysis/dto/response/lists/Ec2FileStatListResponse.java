package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2FileStatResponse;
import com.cirf.dashboard.domain.analysis.dto.response.PageableDto;
import lombok.Builder;

import java.util.List;

@Builder
public record Ec2FileStatListResponse(
        List<Ec2FileStatResponse> logs,
        PageableDto pageable
) {
    public static Ec2FileStatListResponse of(SliceWithSort<Ec2FileStatResponse> sliceWithSort) {
        long totalElements = sliceWithSort.totalElements();
        int pageSize = sliceWithSort.pageSize();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        return Ec2FileStatListResponse.builder()
                .logs(sliceWithSort.content())
                .pageable(PageableDto.builder()
                        .pageNumber(sliceWithSort.pageNumber())
                        .pageSize(pageSize)
                        .numberOfElements(null)
                        .totalPages(totalElements > 0 ? totalPages : null)
                        .totalElements(totalElements > 0 ? totalElements : null)
                        .isLast(!sliceWithSort.hasNext())
                        .nextSearchAfter(null)
                        .build())
                .build();
    }
}
