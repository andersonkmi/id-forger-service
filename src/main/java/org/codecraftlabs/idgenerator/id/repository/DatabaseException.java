package org.codecraftlabs.idgenerator.id.repository;

/**
 * Unchecked exception thrown when a database operation fails.
 */
public class DatabaseException extends RuntimeException {
    /**
     * @param message description of the failure
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * @param message   description of the failure
     * @param exception the underlying cause
     */
    public DatabaseException(String message, Throwable exception) {
        super(message, exception);
    }
}
