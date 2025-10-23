package com.cirf.dashboard.domain.cases.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    CASE_CREATED("사례가 등록되었습니다.");

    private final String message;
}
