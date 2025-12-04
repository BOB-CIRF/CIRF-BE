package com.cirf.dashboard.domain.analysis.dto;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record SliceWithSort<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        boolean hasNext,
        String lastSortValue,  // 예: "1733220000000_sort_id_123"
        long totalElements   // 전체 개수
) {
    public static <T> SliceWithSort<T> of(Slice<T> slice, String lastSortValue, long totalElements) {
        return SliceWithSort.<T>builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .hasNext(slice.hasNext())
                .lastSortValue(lastSortValue)
                .totalElements(totalElements)
                .build();
    }
}
