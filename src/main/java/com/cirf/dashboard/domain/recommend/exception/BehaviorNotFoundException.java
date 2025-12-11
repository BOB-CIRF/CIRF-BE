package com.cirf.dashboard.domain.recommend.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class BehaviorNotFoundException extends BaseException {
    public BehaviorNotFoundException(ErrorMessage errorMessage) {
        super(HttpStatus.NOT_FOUND, errorMessage.getMessage());
    }
}
