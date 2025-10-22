package com.cirf.dashboard.domain.scan.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidRegionException extends BaseException {
    public InvalidRegionException(ErrorMessage e) {
        super(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
