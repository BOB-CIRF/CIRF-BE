package com.cirf.dashboard.domain.collect.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class NotFoundCollectJobException extends BaseException {
    public NotFoundCollectJobException(ErrorMessage ex) {
        super(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
