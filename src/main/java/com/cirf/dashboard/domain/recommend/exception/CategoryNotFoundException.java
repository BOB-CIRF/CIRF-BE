package com.cirf.dashboard.domain.recommend.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends BaseException {
    public CategoryNotFoundException(ErrorMessage errorMessage) {
        super(HttpStatus.NOT_FOUND, errorMessage.getMessage());
    }
}
