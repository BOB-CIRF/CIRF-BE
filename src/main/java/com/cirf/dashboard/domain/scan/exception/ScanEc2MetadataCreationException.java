package com.cirf.dashboard.domain.scan.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ScanEc2MetadataCreationException extends BaseException {

    public ScanEc2MetadataCreationException(ErrorMessage errorMessage) {
        super(HttpStatus.NOT_FOUND, errorMessage.getMessage());
    }
}
