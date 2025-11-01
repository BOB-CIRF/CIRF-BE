package com.cirf.dashboard.domain.analysis.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    LOG_NOT_FOUND("해당 로그를 찾을 수 없습니다.");

    private final String message;
}
