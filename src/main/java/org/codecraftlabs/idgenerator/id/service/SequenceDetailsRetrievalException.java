package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when retrieving sequence metadata fails due to a database error.
 */
public class SequenceDetailsRetrievalException extends RuntimeException {
    /**
     * @param message   description of the failure
     * @param exception the underlying cause
     */
    public SequenceDetailsRetrievalException(String message, Throwable exception) {
        super(message, exception);
    }
}
