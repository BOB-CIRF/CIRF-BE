package com.cirf.dashboard.domain.analysis.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UnauthorizedCaseAccessException extends BaseException {
    public UnauthorizedCaseAccessException(ErrorMessage e) {
        super(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
