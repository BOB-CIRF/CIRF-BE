package com.cirf.dashboard.global.common.dto;

import org.springframework.http.HttpStatus;

public record ExceptionResponse (
        int status,
        String message
)
{
    public static ExceptionResponse of(HttpStatus status, String message) {
        return new ExceptionResponse(status.value(), message);
    }

    public static ExceptionResponse of(int code, String message) {
        return of(HttpStatus.valueOf(code), message);
    }
}

