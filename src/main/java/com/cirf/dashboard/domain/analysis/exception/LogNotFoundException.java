package com.cirf.dashboard.domain.analysis.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class LogNotFoundException extends BaseException {
    public LogNotFoundException(ErrorMessage e) {
        super(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
