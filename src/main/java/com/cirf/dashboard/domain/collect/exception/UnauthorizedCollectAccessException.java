package com.cirf.dashboard.domain.collect.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UnauthorizedCollectAccessException extends BaseException {
    public UnauthorizedCollectAccessException() {
        super(HttpStatus.FORBIDDEN, ErrorMessage.COLLECT_ACCESS_DENIED.getMessage());
    }
}
