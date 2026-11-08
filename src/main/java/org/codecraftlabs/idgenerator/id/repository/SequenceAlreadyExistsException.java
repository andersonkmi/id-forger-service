package org.codecraftlabs.idgenerator.id.repository;

/**
 * Thrown when attempting to create a sequence that already exists in the database.
 */
public class SequenceAlreadyExistsException extends DatabaseException {
    /**
     * @param message   description of the conflict
     * @param exception the underlying cause
     */
    public SequenceAlreadyExistsException(String message, Throwable exception) {
        super(message, exception);
    }
}
