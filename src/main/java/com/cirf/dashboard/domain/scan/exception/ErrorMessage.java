package com.cirf.dashboard.domain.scan.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    SCAN_NOT_FOUND("해당 스캔을 찾을 수 없습니다."),
    SCAN_NOT_COMPLETED("스캔이 아직 완료되지 않았습니다."),
    REGION_NOT_FOUND("해당 리전에 대한 스캔 결과를 찾을 수 없습니다."),
    INTERNAL_ERROR("내부 서버 오류가 발생했습니다.");

    private final String message;
}
