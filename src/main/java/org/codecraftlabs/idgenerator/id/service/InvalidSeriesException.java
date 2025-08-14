package org.codecraftlabs.idgenerator.id.service;

public class InvalidSeriesException extends RuntimeException {
  public InvalidSeriesException(String message) {
    super(message);
  }

  public InvalidSeriesException(String message, Throwable exception) {
    super(message, exception);
  }
}
