package com.cirf.dashboard.domain.collect.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException() {
        super(HttpStatus.FORBIDDEN, ErrorMessage.UNAUTHORIZED_ACCESS.getMessage());
    }

    public UnauthorizedException(ErrorMessage e) {
        super(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
