package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when an ID cannot be generated, typically due to a database failure.
 */
public class IdNotGeneratedException extends RuntimeException {
    /**
     * @param message   description of the failure
     * @param exception the underlying cause
     */
    public IdNotGeneratedException(String message, Throwable exception) {
        super(message, exception);
    }
}
