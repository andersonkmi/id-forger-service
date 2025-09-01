package org.codecraftlabs.idgenerator.id.service;

public class SequenceLastValueUpdateFailedException extends RuntimeException {
    public SequenceLastValueUpdateFailedException(String message, Throwable exception) {
        super(message, exception);
    }
}
