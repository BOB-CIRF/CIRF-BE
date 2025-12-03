package com.cirf.dashboard.domain.analysis.dto.response;

import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Builder
public record AwsNativeLogResponse(
        String id,
        String accountId,
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
    public static AwsNativeLogResponse from(LogEvent event) {
        // Use timestamp field (which maps to @timestamp in ES) as the primary time source
        Instant instant = event.getTimestamp();
        LocalDateTime time = instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;

        return AwsNativeLogResponse.builder()
                .id(event.getId())
                .accountId(event.getAccountId())
                .time(time)
                .type(event.getType())
                .actor(event.getActor())
                .activity(event.getActivity())
                .target(event.getTarget())
                .src(event.getSrc())
                .dst(event.getDst())
                .outcome(event.getOutcome())
                .region(event.getRegion())
                .build();
    }
}
