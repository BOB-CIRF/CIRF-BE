package com.cirf.dashboard.domain.cases.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    CASE_NOT_FOUND("해당 케이스를 찾을 수 없습니다.");

    private final String message;
}
