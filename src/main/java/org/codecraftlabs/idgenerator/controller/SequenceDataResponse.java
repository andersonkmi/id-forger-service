package org.codecraftlabs.idgenerator.controller;

/**
 * Response carrying sequence metadata returned by the {@code /details} endpoint.
 */
public record SequenceDataResponse(String schema, String name, Long startValue, Long minValue, Long maxValue,
                                   Long incrementBy, Long cacheSize, Boolean cycle, Long lastValue) {
}
