package org.codecraftlabs.idgenerator.id;

/**
 * Immutable value object representing a PostgreSQL sequence and its configuration metadata.
 */
public class Sequence {
    private final String schema;
    private final String name;
    private final Long startValue;
    private final Long minValue;
    private final Long maxValue;
    private final Long incrementBy;
    private final Long cacheSize;
    private final Boolean cycle;
    private final Long lastValue;

    /**
     * Constructs a fully populated Sequence.
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
    public Sequence(String schema, String name, Long startValue, Long minValue, Long maxValue, Long incrementBy, Long cacheSize, Boolean cycle, Long lastValue) {
        this.schema = schema;
        this.name = name;
        this.startValue = startValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.incrementBy = incrementBy;
        this.cacheSize = cacheSize;
        this.cycle = cycle;
        this.lastValue = lastValue;
    }

    /** @return database schema that owns the sequence */
    public String getSchema() {
        return schema;
    }

    /** @return sequence name */
    public String getName() {
        return name;
    }

    /** @return value the sequence started at */
    public Long getStartValue() {
        return startValue;
    }

    /** @return minimum allowed value */
    public Long getMinValue() {
        return minValue;
    }

    /** @return maximum allowed value */
    public Long getMaxValue() {
        return maxValue;
    }

    /** @return step size between consecutive values */
    public Long getIncrementBy() {
        return incrementBy;
    }

    /** @return number of values pre-allocated in memory by the database */
    public Long getCacheSize() {
        return cacheSize;
    }

    /** @return {@code true} if the sequence wraps around after reaching its maximum value */
    public Boolean isCycle() {
        return cycle;
    }

    /** @return most recently generated value */
    public Long getLastValue() {
        return lastValue;
    }
}
