package com.cirf.dashboard.domain.scan.dto.response;

public record ScanEc2Response(
        Long id,
        String accountId,
        String instanceId,
        String instanceName,
        String instanceType,
        String region,
        String status,
        String platformDetails,
        String publicIp
) {
}
