package com.cirf.dashboard.domain.cases.exception;

public class NotFoundAccountException extends RuntimeException {
  public NotFoundAccountException(String message) {
    super(message);
  }
}
