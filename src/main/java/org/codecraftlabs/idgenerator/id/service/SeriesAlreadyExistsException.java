package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when creating a series whose name, or backing sequence, already exists.
 * A client-side conflict rather than a server failure, mapped to HTTP 409.
 */
public class SeriesAlreadyExistsException extends SeriesNotCreatedException {
    /**
     * @param message   description of the conflict
     * @param exception the underlying cause
     */
    public SeriesAlreadyExistsException(String message, Throwable exception) {
        super(message, exception);
    }
}
