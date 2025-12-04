package com.cirf.dashboard.domain.analysis.dto.response;

import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import lombok.Builder;

@Builder
public record Ec2LogDetailResponse(
        String id,
        String rawData  // event.original을 String으로 반환
) {
    public static Ec2LogDetailResponse from(Ec2LogEvent event) {
        return Ec2LogDetailResponse.builder()
                .id(event.getId())
                .rawData(event.getRaw())
                .build();
    }
}
