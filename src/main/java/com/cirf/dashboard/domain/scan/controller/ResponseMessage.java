package com.cirf.dashboard.domain.scan.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    GET_SCAN_RESULTS_SUCCESS("해당 account의 리전의 스캔 결과를 성공적으로 조회했습니다.");

    private final String message;
}
