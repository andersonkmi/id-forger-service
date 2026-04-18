package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when a requested series name does not exist or is not mapped to a sequence.
 */
public class InvalidSeriesException extends RuntimeException {
    /**
     * @param message description of the invalid series
     */
  public InvalidSeriesException(String message) {
    super(message);
  }

    /**
     * @param message   description of the invalid series
     * @param exception the underlying cause
     */
  public InvalidSeriesException(String message, Throwable exception) {
    super(message, exception);
  }
}
