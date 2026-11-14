package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when a new series cannot be created. The subclass
 * {@link SeriesAlreadyExistsException} signals the conflict case.
 */
public class SeriesNotCreatedException extends RuntimeException {
    /**
     * @param message   description of the failure
     * @param exception the underlying cause
     */
    public SeriesNotCreatedException(String message, Throwable exception) {
        super(message, exception);
    }
}
