package com.cirf.dashboard.domain.collect.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    COLLECT_JOB_NOT_FOUND("해당 수집 작업을 찾을 수 없습니다."),
    COLLECT_ACCESS_DENIED("해당 수집 작업에 접근 권한이 없습니다.");

    private final String message;
}
