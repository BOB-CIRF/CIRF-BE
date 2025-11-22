package com.cirf.dashboard.domain.auth.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, ErrorMessage.USER_UNAUTHORIZED.getMessage());
    }
}
