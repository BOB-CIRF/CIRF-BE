package com.cirf.dashboard.domain.scan.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AwsEc2Exception extends BaseException {
    public AwsEc2Exception(ErrorMessage errorMessage) {
      super(HttpStatus.NOT_FOUND, errorMessage.getMessage());
    }
}
