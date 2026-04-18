package org.codecraftlabs.idgenerator.id.service.processor;

import jakarta.annotation.Nonnull;

/**
 * Strategy interface for ID format processors.
 * Each implementation generates an ID for a series and applies a specific
 * formatting transformation (e.g. plain numeric, Base64, prefixed, Luhn-checked).
 */
public interface IdFormatProcessor {
    /**
     * Generates and formats the next ID for the given series.
     *
     * @param seriesName the series name
     * @return the formatted ID string
     */
    @Nonnull
    String generateId(@Nonnull String seriesName);
}
