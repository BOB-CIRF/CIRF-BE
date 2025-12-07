package com.cirf.dashboard.domain.collect.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    GET_COLLECT_PROCESS_SUCCESS("수집 작업 진행도를 성공적으로 조회했습니다."),
    GET_COLLECT_LIST_SUCCESS("수집 작업 목록을 성공적으로 조회했습니다."),
    SAVE_COLLECT_PROGRESS_SUCCESS("수집 진행도 관련 리소스를 저장했습니다."),;

    private final String message;
}
