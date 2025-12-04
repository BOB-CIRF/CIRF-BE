package com.cirf.dashboard.domain.analysis.dto.response;

public record Ec2FileStatResponse(
        String instanceId,
        String fileName,
        String fileSize,
        String permissions,
        String accessTime,
        String modifyTime,
        String changeTime
) {
}
