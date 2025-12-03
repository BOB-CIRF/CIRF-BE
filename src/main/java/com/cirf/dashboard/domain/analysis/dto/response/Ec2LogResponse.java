package com.cirf.dashboard.domain.analysis.dto.response;

import com.cirf.dashboard.domain.analysis.entity.Ec2LogEvent;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Builder
public record Ec2LogResponse(
        String id,
        String accountId,
        String instanceId,
        LocalDateTime time,
        String fileName,
        String logType,
        String activity,
        String outcome,
        String region
) {
    public static Ec2LogResponse from(Ec2LogEvent event) {
        // Use timestamp field (which maps to @timestamp in ES) as the primary time source
        Instant instant = event.getTimestamp();
        LocalDateTime time = instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;

        // fileName 변환: var_log_wtmp.json.gz -> /var/log/wtmp
        String fileName = convertFileName(event.getFileName());

        return Ec2LogResponse.builder()
                .id(event.getId())
                .accountId(event.getAccountId())
                .instanceId(event.getInstanceId())
                .time(time)
                .fileName(fileName)
                .logType(event.getLogType())
                .activity(event.getActivity())
                .outcome(event.getOutcome())
                .region(event.getRegion())
                .build();
    }

    /**
     * fileName 변환: var_log_wtmp.json.gz -> /var/log/wtmp
     */
    private static String convertFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return fileName;
        }

        // .json.gz 제거
        String result = fileName.replaceAll("\\.json\\.gz$", "");

        // _ 를 / 로 변환
        result = result.replace("_", "/");

        // 맨 앞에 / 추가 (없으면)
        if (!result.startsWith("/")) {
            result = "/" + result;
        }

        return result;
    }
}
