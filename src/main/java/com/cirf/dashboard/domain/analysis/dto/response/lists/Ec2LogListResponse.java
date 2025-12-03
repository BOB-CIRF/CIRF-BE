package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.response.Ec2LogResponse;
import com.cirf.dashboard.domain.analysis.dto.response.PageableDto;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record Ec2LogListResponse(
        List<Ec2LogResponse> logs,
        PageableDto pageable
) {
    public static Ec2LogListResponse of(Slice<Ec2LogResponse> slice) {
        return Ec2LogListResponse.builder()
                .logs(slice.getContent())
                .pageable(PageableDto.builder()
                        .pageNumber(slice.getNumber())
                        .pageSize(slice.getSize())
                        .isLast(slice.isLast())
                        .build())
                .build();
    }
}
