package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when updating a sequence's last value fails due to a database error.
 */
public class SequenceLastValueUpdateFailedException extends RuntimeException {
    /**
     * @param message   description of the failure
     * @param exception the underlying cause
     */
    public SequenceLastValueUpdateFailedException(String message, Throwable exception) {
        super(message, exception);
    }
}
