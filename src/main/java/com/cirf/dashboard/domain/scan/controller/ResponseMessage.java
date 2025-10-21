package com.cirf.dashboard.domain.scan.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    SCAN_STATUS_RETRIEVED("스캔 상태를 성공적으로 조회했습니다");

    private final String message;
}
