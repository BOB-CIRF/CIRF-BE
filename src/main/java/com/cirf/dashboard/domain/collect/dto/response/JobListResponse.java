package com.cirf.dashboard.domain.collect.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record JobListResponse(
        List<JobDetailResponse> content,
        PageableResponse pageable
) {
    public static JobListResponse of(Slice<JobDetailResponse> jobs) {
        return JobListResponse.builder()
                .content(jobs.getContent())
                .pageable(PageableResponse.of(jobs))
                .build();
    }
}
