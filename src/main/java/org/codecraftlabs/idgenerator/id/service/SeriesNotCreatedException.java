package org.codecraftlabs.idgenerator.id.service;

public class SeriesNotCreatedException extends RuntimeException {
    public SeriesNotCreatedException(String message, Throwable exception) {
        super(message, exception);
    }
}
