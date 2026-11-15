package org.codecraftlabs.idgenerator.common.repository;

/**
 * Thrown when a requested sequence does not exist in the database.
 */
public class SequenceNotFoundException extends DatabaseException {
    /**
     * @param message description of the missing sequence
     */
    public SequenceNotFoundException(String message) {
        super(message);
    }
}
