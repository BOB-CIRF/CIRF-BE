package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.SliceWithSort;
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
    public static AwsNativeLogListResponse of(SliceWithSort<AwsNativeLogResponse> sliceWithSort) {
        return AwsNativeLogListResponse.builder()
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
