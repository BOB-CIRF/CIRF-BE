package com.cirf.dashboard.domain.collect.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ProgressNotFoundException extends BaseException {
    public ProgressNotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorMessage.PROGRESS_NOT_FOUND.getMessage());
    }

    public ProgressNotFoundException(ErrorMessage e) {
        super(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
