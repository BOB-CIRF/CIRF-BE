package com.cirf.dashboard.domain.scan.dto.response.lists;

import com.cirf.dashboard.domain.scan.dto.response.PageableResponse;
import com.cirf.dashboard.domain.scan.dto.response.ScanEc2Response;
import lombok.Builder;
import org.springframework.data.domain.Slice;

import java.util.List;

@Builder
public record ScanEc2ListResponse(
        List<ScanEc2Response> ec2Lists,
        PageableResponse pageable
) {
    public static ScanEc2ListResponse of(Slice<ScanEc2Response> slice) {
        return ScanEc2ListResponse.builder()
                .ec2Lists(slice.getContent())
                .pageable(PageableResponse.of(slice))
                .build();
    }
}
