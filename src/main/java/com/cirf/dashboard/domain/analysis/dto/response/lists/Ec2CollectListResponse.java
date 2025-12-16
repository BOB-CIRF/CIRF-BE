package com.cirf.dashboard.domain.analysis.dto.response.lists;

import com.cirf.dashboard.domain.analysis.dto.response.Ec2CollectResponse;
import com.cirf.dashboard.domain.analysis.dto.response.PageableDto;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record Ec2CollectListResponse(
        List<Ec2CollectResponse> ec2List,
        PageableDto pageable
) {
    public static Ec2CollectListResponse of(Page<Ec2CollectResponse> page) {
        return Ec2CollectListResponse.builder()
                .ec2List(page.getContent())
                .pageable(PageableDto.builder()
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .numberOfElements(page.getNumberOfElements())
                        .totalPages(page.getTotalPages())
                        .totalElements(page.getTotalElements())
                        .isLast(page.isLast())
                        .build())
                .build();

    }
}
