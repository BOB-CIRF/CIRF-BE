package com.cirf.dashboard.domain.cases.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class NotFoundAccountException extends BaseException {
    public NotFoundAccountException(ErrorMessage e) {
        super(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
