package org.codecraftlabs.idgenerator.controller.admin;

/**
 * Request body used when updating the last value of a sequence via the PUT endpoint.
 *
 * @param newLastValue the desired last value
 */
public record SeriesLastValue(long newLastValue) {
}
