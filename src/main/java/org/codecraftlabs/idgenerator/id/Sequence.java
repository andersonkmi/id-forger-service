package org.codecraftlabs.idgenerator.id;

/**
 * Immutable value object representing a PostgreSQL sequence and its configuration metadata.
 *
 * @param schema      database schema that owns the sequence
 * @param name        sequence name
 * @param startValue  value the sequence starts at
 * @param minValue    minimum allowed value
 * @param maxValue    maximum allowed value
 * @param incrementBy step size between consecutive values
 * @param cacheSize   number of values pre-allocated in memory by the database
 * @param cycle       {@code true} if the sequence wraps around after reaching its max value
 * @param lastValue   most recently generated value
 */
public record Sequence(String schema, String name, Long startValue, Long minValue, Long maxValue,
                       Long incrementBy, Long cacheSize, Boolean cycle, Long lastValue) {
}
