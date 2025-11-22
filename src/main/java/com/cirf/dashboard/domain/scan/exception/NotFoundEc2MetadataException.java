package com.cirf.dashboard.domain.scan.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class NotFoundEc2MetadataException extends BaseException {
    public NotFoundEc2MetadataException(ErrorMessage e) {
        super(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
