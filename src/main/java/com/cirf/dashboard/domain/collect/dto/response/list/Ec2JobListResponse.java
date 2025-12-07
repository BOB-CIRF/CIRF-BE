package com.cirf.dashboard.domain.collect.dto.response.list;

import com.cirf.dashboard.domain.collect.dto.response.Ec2JobResponse;
import com.cirf.dashboard.domain.collect.dto.response.PageableResponse;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record Ec2JobListResponse(
        List<Ec2JobResponse> content,
        PageableResponse pageable
) {
    public static Ec2JobListResponse of(Slice<Ec2JobResponse> jobs) {
        return Ec2JobListResponse.builder()
                .content(jobs.getContent())
                .pageable(PageableResponse.of(jobs))
                .build();
    }
}
