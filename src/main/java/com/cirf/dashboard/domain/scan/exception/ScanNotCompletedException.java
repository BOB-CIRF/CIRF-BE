package com.cirf.dashboard.domain.scan.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ScanNotCompletedException extends BaseException {
    public ScanNotCompletedException(ErrorMessage errorMessage) {
        super(HttpStatus.BAD_REQUEST, errorMessage.getMessage());
    }
}
