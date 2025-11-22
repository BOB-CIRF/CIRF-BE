package com.cirf.dashboard.domain.analysis.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    GET_LOGS_SUCCESS("로그 조회에 성공했습니다."),
    GET_RAW_LOG_SUCCESS("로그 원본 데이터 조회에 성공했습니다."),
    DOWNLOAD_LOG_SUCCESS("로그 다운로드에 성공했습니다.");

    private final String message;
}
