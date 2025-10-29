package com.cirf.dashboard.domain.cases.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdateRequest {
    private String caseName;
    private String caseDescription;
    private List<String> accountIds;
}