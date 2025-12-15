package com.cirf.dashboard.domain.collect.dto.response;

import com.cirf.dashboard.domain.collect.entity.CollectJob;
import lombok.Builder;

@Builder
public record AwsNativeJobDetailResponse(
        String jobId,
        String logType,
        String dt,
        String startedAt,
        String lastUpdated,
        String status,
        String description
) {
    public static AwsNativeJobDetailResponse from(CollectJob job) {
        String description = createDescription(job.getDt(), job.getStatus());

        return AwsNativeJobDetailResponse.builder()
                .jobId(job.getJobId())
                .logType(job.getLogType())
                .dt(job.getDt())
                .startedAt(job.getStartedAt())
                .lastUpdated(job.getLastUpdated())
                .status(job.getStatus())
                .description(description)
                .build();
    }

    private static String createDescription(String dt, String status) {
        if (dt == null || status == null) {
            return "알 수 없는 상태";
        }

        String statusDisplay = getStatusDisplay(status);

        return String.format("%s 수집 %s", dt, statusDisplay);
    }

    private static String getStatusDisplay(String status) {
        return switch (status.toLowerCase()) {
            case "pending" -> "대기 중";
            case "inprogress", "in_progress" -> "진행 중";
            case "skipped" -> "스킵";
            case "completed" -> "완료";
            case "fail", "failed" -> "실패";
            case "nodata" -> "데이터 없음";
            default -> status;
        };
    }
}
