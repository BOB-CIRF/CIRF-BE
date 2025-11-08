package com.cirf.dashboard.domain.collect.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record JobListResponse(
        List<JobDetailResponse> content,
        PageableResponse pageable
) {
    @Builder
    public record PageableResponse(
            int pageNumber,
            int pageSize,
            int numberOfElements,
            boolean isLast
    ) {
        public static PageableResponse of(int page, int size, int totalElements, int currentElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean isLast = page >= totalPages - 1 || totalPages == 0;

            return PageableResponse.builder()
                    .pageNumber(page)
                    .pageSize(size)
                    .numberOfElements(currentElements)
                    .isLast(isLast)
                    .build();
        }
    }

    public static JobListResponse of(List<JobDetailResponse> allJobs, int page, int size) {
        int totalElements = allJobs.size();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allJobs.size());

        List<JobDetailResponse> content = fromIndex < allJobs.size()
                ? allJobs.subList(fromIndex, toIndex)
                : List.of();

        PageableResponse pageable = PageableResponse.of(page, size, totalElements, content.size());

        return JobListResponse.builder()
                .content(content)
                .pageable(pageable)
                .build();
    }
}
