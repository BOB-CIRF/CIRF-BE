package com.cirf.dashboard.domain.analysis.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class FailedDecompressFileException extends BaseException {
    public FailedDecompressFileException(ErrorMessage ex) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
