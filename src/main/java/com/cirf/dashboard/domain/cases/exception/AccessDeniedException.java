package com.cirf.dashboard.domain.cases.exception;

import lombok.Getter;

/**
 * 접근 권한 예외
 * HTTP 403 Forbidden 응답을 반환하는 경우 사용
 */
@Getter
public class AccessDeniedException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public AccessDeniedException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }

    public AccessDeniedException(String message) {
        super(message);
        this.errorMessage = ErrorMessage.ACCESS_DENIED;
    }
}