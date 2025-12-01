package com.cirf.dashboard.domain.cases.exception;

public class ExistsAccountIdException extends RuntimeException {
  public ExistsAccountIdException(String message) {
    super(message);
  }
}
