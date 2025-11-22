package com.cirf.dashboard.domain.analysis.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends BaseException {
    public InvalidDateRangeException(ErrorMessage e) {
        super(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
