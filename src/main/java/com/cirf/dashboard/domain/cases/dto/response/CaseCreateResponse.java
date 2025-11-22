package com.cirf.dashboard.domain.cases.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CaseCreateResponse {

    @JsonProperty("caseId")
    private Long caseId;
}