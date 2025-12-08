package com.cirf.dashboard.domain.cases.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingInfoResponse {

    /**
     * CloudFormation 템플릿 내용
     */
    private String templateContent;

    /**
     * S3 Presigned URL (템플릿 다운로드용)
     */
    private String presignedUrl;

    /**
     * CloudFormation 런치 링크
     */
    private String launchUrl;

    /**
     * 고객 AWS 계정 ID
     */
    private String accountId;

    /**
     * 사례 ID
     */
    private Long caseId;

    /**
     * Presigned URL 만료 시간 (분)
     */
    private Integer expirationMinutes;
}