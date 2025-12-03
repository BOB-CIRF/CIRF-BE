package com.cirf.dashboard.domain.analysis.dto.response;

import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Builder
public record Ec2LogDetailResponse(
        String id,
        Object rawData  // JSON 객체로 파싱된 event.original
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Ec2LogDetailResponse from(Ec2LogEvent event) {
        System.out.println(event.getRaw());
        // raw data JSON 파싱
        Object rawData = parseRawData(event.getRaw());

        return Ec2LogDetailResponse.builder()
                .id(event.getId())
                .rawData(rawData)
                .build();
    }

    private static Object parseRawData(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            // JSON 문자열을 Object로 파싱 (Map 또는 List가 될 수 있음)
            return objectMapper.readValue(raw, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse raw data as JSON, returning as string: {}", e.getMessage());
            // JSON 파싱 실패 시 원본 문자열 그대로 반환
            return raw;
        }
    }
}
