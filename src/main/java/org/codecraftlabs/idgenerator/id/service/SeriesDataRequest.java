package org.codecraftlabs.idgenerator.id.service;

/**
 * Service-layer request describing a new series to be created.
 *
 * <p>The numeric parameters are optional; when {@code null}, the database default
 * for the corresponding sequence option is used.</p>
 *
 * @param name          the series name, used as the name of the underlying sequence
 * @param prefix        the prefix applied to identifiers generated for this series
 * @param startingValue the value the sequence starts at, or {@code null} for the database default
 * @param incrementBy   the step size between consecutive values, or {@code null} for the database default
 * @param minimumValue  the minimum allowed value, or {@code null} for the database default
 * @param maximumValue  the maximum allowed value, or {@code null} for the database default
 */
public record SeriesDataRequest(String name,
                                String prefix,
                                Long startingValue,
                                Long incrementBy,
                                Long minimumValue,
                                Long maximumValue) {
}
