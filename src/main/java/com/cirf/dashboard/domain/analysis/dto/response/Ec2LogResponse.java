package com.cirf.dashboard.domain.analysis.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record Ec2LogResponse(
        String id,
        String accountId,
        String instanceId,
        LocalDateTime time,
        String type,
        String actor,
        String activity,
        String target,
        String src,
        String dst,
        String outcome,
        String region
) {
}
