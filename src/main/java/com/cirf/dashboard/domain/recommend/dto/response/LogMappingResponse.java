package com.cirf.dashboard.domain.recommend.dto.response;

import com.cirf.dashboard.domain.recommend.entity.LogMapping;
import lombok.Builder;

@Builder
public record LogMappingResponse(
        String logType,
        String displayName,
        String reason
) {
    public static LogMappingResponse from(LogMapping logMapping) {
        return LogMappingResponse.builder()
                .logType(logMapping.getLogType())
                .displayName(logMapping.getDisplayName())
                .reason(logMapping.getReason())
                .build();
    }
}
