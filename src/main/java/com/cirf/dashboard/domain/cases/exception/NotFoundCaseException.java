package com.cirf.dashboard.domain.cases.exception;

public class NotFoundCaseException extends RuntimeException {
  public NotFoundCaseException(String message) {
    super(message);
  }
}
