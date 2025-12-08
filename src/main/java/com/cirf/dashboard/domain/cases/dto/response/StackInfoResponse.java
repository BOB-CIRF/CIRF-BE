package com.cirf.dashboard.domain.cases.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StackInfoResponse {

    private String stackName;
    private String stackStatus;
    private String createdAt;
    private String updatedAt;
    private Map<String, String> parameters;
    private Map<String, String> outputs;
    private String statusReason;
}