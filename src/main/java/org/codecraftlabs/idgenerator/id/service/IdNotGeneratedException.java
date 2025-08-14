package org.codecraftlabs.idgenerator.id.service;

public class IdNotGeneratedException extends RuntimeException {
    public IdNotGeneratedException(String message, Throwable exception) {
        super(message, exception);
    }
}
