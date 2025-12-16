package com.cirf.dashboard.domain.analysis.dto.response;

public record Ec2CollectResponse(
        String instanceId,
        String instanceName,
        String instanceType,
        String region,
        String accountId
) {
}
