package com.cirf.dashboard.domain.cases.dto.response;

import com.cirf.dashboard.domain.cases.entity.DeploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentStatusResponse {

    private Long caseId;
    private String accountId;
    private String stackName;
    private String deploymentStatus;
    private String statusReason;
    private String roleArn;
    private String createdAt;
    private String updatedAt;

    public static DeploymentStatusResponse from(DeploymentStatus entity) {
        return DeploymentStatusResponse.builder()
                .caseId(entity.getCaseId())
                .accountId(entity.getAccountId())
                .stackName(entity.getStackName())
                .deploymentStatus(entity.getStackStatus().name())
                .statusReason(entity.getStatusReason())
                .roleArn(entity.getRoleArn())
                .createdAt(entity.getCreatedAt() != null
                        ? entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .updatedAt(entity.getUpdatedAt() != null
                        ? entity.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .build();
    }
}