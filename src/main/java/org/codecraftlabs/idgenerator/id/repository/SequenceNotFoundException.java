package org.codecraftlabs.idgenerator.id.repository;

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
