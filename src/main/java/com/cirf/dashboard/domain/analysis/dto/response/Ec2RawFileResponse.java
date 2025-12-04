package com.cirf.dashboard.domain.analysis.dto.response;

import lombok.Builder;

@Builder
public record Ec2RawFileResponse(
        String rawContent
) {
}
