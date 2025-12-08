package com.cirf.dashboard.domain.cases.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CaseCreateResponse {

    @JsonProperty("caseId")
    private Long caseId;
    private List<String> cloudFormationTemplateUrls;
    // 기존 생성자 호환성 유지
    public CaseCreateResponse(Long caseId) {
        this.caseId = caseId;
    }
}