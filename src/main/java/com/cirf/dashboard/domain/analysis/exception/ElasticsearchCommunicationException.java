package com.cirf.dashboard.domain.analysis.exception;

import com.cirf.dashboard.global.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ElasticsearchCommunicationException extends BaseException {
    public ElasticsearchCommunicationException(ErrorMessage e) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}
