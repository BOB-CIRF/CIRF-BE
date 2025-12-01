package com.cirf.dashboard.domain.cases.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ExistsAccountIdException extends BaseException {
    public ExistsAccountIdException(ErrorMessage ex) {
        super(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
