package com.cirf.dashboard.domain.analysis.dto.response;

import com.cirf.dashboard.domain.analysis.entity.LogEvent;
import lombok.Builder;

import java.util.Map;

/**
 * Response DTO for raw log data
 */
@Builder
public record LogRawDataResponse(
        String id,
        Map<String, Object> rawData
) {
    public static LogRawDataResponse from(LogEvent event) {
        return LogRawDataResponse.builder()
                .id(event.getId())
                .rawData(event.getEvent_data())
                .build();
    }
}
