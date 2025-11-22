package com.cirf.dashboard.domain.cases.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class NotFoundBucketException extends BaseException {
    public NotFoundBucketException(ErrorMessage e) {
        super(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
