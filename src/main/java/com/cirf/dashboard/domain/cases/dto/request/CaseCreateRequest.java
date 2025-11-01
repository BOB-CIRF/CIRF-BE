package com.cirf.dashboard.domain.cases.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseCreateRequest {

    @NotBlank(message = "caseName은 필수입니다.")
    private String caseName;

    @NotBlank(message = "caseDescription은 필수입니다.")
    private String caseDescription;

    @NotEmpty(message = "계정 ID 목록은 비어있을 수 없습니다.")
    private List<String> accountIds;
}