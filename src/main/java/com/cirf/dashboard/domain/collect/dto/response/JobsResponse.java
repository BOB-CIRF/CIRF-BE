package com.cirf.dashboard.domain.collect.dto.response;

import com.cirf.dashboard.domain.collect.dto.request.WebhookJob;

import java.util.List;

/**
 * 작업 목록 조회 응답 (페이지네이션)
 */
public record JobsResponse(
        List<WebhookJob> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static JobsResponse of(List<WebhookJob> jobs, int page, int size) {
        long totalElements = jobs.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, jobs.size());

        List<WebhookJob> content = fromIndex < jobs.size()
                ? jobs.subList(fromIndex, toIndex)
                : List.of();

        return new JobsResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
}
