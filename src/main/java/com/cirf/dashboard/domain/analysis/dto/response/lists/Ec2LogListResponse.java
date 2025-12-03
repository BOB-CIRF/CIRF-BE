package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogResponse;
import com.cirf.dashboard.domain.analysis.dto.response.PageableDto;
import lombok.Builder;

import java.util.List;

@Builder
public record Ec2LogListResponse(
        List<Ec2LogResponse> logs,
        PageableDto pageable
) {
    public static Ec2LogListResponse of(SliceWithSort<Ec2LogResponse> sliceWithSort) {
        return Ec2LogListResponse.builder()
                .logs(sliceWithSort.content())
                .pageable(PageableDto.builder()
                        .pageNumber(null)
                        .pageSize(sliceWithSort.pageSize())
                        .numberOfElements(null)
                        .totalPages(null)
                        .totalElements(sliceWithSort.totalElements() > 0 ? sliceWithSort.totalElements() : null)  // 첫 페이지만
                        .isLast(!sliceWithSort.hasNext())
                        .nextSearchAfter(sliceWithSort.lastSortValue())
                        .build())
                .build();
    }
}
