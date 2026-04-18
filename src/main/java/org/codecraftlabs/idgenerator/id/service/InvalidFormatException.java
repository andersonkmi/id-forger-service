package org.codecraftlabs.idgenerator.id.service;

/**
 * Thrown when a caller requests a format that has no registered {@code IdFormatProcessor}.
 */
public class InvalidFormatException extends RuntimeException {
    /**
     * @param message description of the invalid format
     */
    public InvalidFormatException(String message) {
        super(message);
    }
}
