package com.cirf.dashboard.domain.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {

    USER_UNAUTHORIZED("존재하지 않는 유저입니다.");

    private final String message;
}