package com.cirf.dashboard.domain.scan.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CustomAccessDeniedException extends BaseException {
    public CustomAccessDeniedException(ErrorMessage e) {
        super(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
