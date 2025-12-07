package com.cirf.dashboard.domain.collect.dto.response.list;

import com.cirf.dashboard.domain.collect.dto.response.AwsNativeJobDetailResponse;
import com.cirf.dashboard.domain.collect.dto.response.PageableResponse;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record AwsNativeJobListResponse(
        List<AwsNativeJobDetailResponse> content,
        PageableResponse pageable
) {
    public static AwsNativeJobListResponse of(Slice<AwsNativeJobDetailResponse> jobs) {
        return AwsNativeJobListResponse.builder()
                .content(jobs.getContent())
                .pageable(PageableResponse.of(jobs))
                .build();
    }
}
