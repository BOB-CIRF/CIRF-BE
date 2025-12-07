package com.cirf.dashboard.domain.collect.dto.response;

import com.cirf.dashboard.domain.collect.entity.CollectEc2Job;
import com.cirf.dashboard.domain.collect.entity.CollectJob;
import lombok.Builder;

@Builder
public record Ec2JobResponse(
        String jobName,
        String instanceId,
        String startedAt,
        String lastUpdated,
        String status,
        String description
) {

    public static Ec2JobResponse from(CollectEc2Job job) {
        String description = createDescription(job.getInstanceId(), job.getSk(), job.getStatus());

        return Ec2JobResponse.builder()
                .jobName(extractJobNameFromSk(job.getSk()))
                .instanceId(job.getInstanceId())
                .startedAt(job.getCreatedAt())
                .lastUpdated(job.getUpdatedAt())
                .status(job.getStatus())
                .description(description)
                .build();
    }

    private static String extractJobNameFromSk(String sk) {
        if (sk == null || !sk.contains("#")) {
            return sk;
        }
        return sk.substring(sk.lastIndexOf("#") + 1);
    }

    private static String createDescription(String instanceId, String sk, String status) {
        if (status == null) {
            return "작업 상태 정보 없음";
        }
        String jobName = getJobName(extractJobNameFromSk(sk));
        String statusDisplay = getStatusDisplay(status);

        return String.format("%s %s %s", instanceId, jobName, statusDisplay);
    }

    private static String getStatusDisplay(String status) {
        return switch (status.toLowerCase()) {
            case "pending" -> "대기 중";
            case "inprogress", "in_progress" -> "진행 중";
            case "completed" -> "완료";
            case "fail", "failed" -> "실패";
            case "nodata" -> "데이터 없음";
            default -> status;
        };
    }

    private static String getJobName(String jobName){
        return switch (jobName) {
            case "LOGCOLLECT" -> "로그 수집";
            case "MEMDUMP" -> "메모리 덤프";
            case "SNAPSHOT_ATTACH" -> "스냅샷 Attach";
            default -> jobName;
        };
    }
}
