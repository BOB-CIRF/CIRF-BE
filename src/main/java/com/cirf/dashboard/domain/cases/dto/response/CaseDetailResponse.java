package com.cirf.dashboard.domain.cases.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDetailResponse {
    private Long caseId;
    private String caseName;
    private String caseDescription;
    private String status;
    private String analystName;
    private List<String> accountIds;

}