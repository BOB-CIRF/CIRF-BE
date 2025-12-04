package com.cirf.dashboard.domain.analysis.dto.response;

import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import lombok.Builder;

import java.util.Map;

/**
 * Response DTO for raw log data
 */
@Builder
public record AwsNativeLogRawDataResponse(
        String id,
        Map<String, Object> rawData
) {
    public static AwsNativeLogRawDataResponse from(LogEvent event) {
        return AwsNativeLogRawDataResponse.builder()
                .id(event.getId())
                .rawData(event.getEvent_data())
                .build();
    }
}
