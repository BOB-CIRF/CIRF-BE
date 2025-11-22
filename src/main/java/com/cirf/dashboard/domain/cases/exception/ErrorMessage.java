package com.cirf.dashboard.domain.cases.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 메시지 관리 Enum
 */
@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    // 400 Bad Request
    INVALID_REQUEST("잘못된 요청입니다."),
    INVALID_ACCOUNT_ID("존재하지 않는 accountId가 포함되어 있습니다."),

    // 403 Forbidden
    ACCESS_DENIED("잘못된 접근 권한입니다."),

    // 404 Not Found
    CASE_NOT_FOUND("사례를 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND("계정을 찾을 수 없습니다."),
    BUCKET_NOT_FOUND("버킷을 찾을 수 없습니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR("서버 내부에 난 오류입니다.");

    private final String message;
}