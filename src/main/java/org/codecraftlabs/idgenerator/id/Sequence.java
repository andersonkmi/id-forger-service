package org.codecraftlabs.idgenerator.id;

/**
 * Immutable value object representing a PostgreSQL sequence and its configuration metadata.
 */
public class Sequence {
    private final String schema;
    private final String name;
    private final long startValue;
    private final long minValue;
    private final long maxValue;
    private final long incrementBy;
    private final long cacheSize;
    private final boolean cycle;
    private final long lastValue;

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
    public Sequence(String schema, String name, long startValue, long minValue, long maxValue, long incrementBy, long cacheSize, boolean cycle, long lastValue) {
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
    public long getStartValue() {
        return startValue;
    }

    /** @return minimum allowed value */
    public long getMinValue() {
        return minValue;
    }

    /** @return maximum allowed value */
    public long getMaxValue() {
        return maxValue;
    }

    /** @return step size between consecutive values */
    public long getIncrementBy() {
        return incrementBy;
    }

    /** @return number of values pre-allocated in memory by the database */
    public long getCacheSize() {
        return cacheSize;
    }

    /** @return {@code true} if the sequence wraps around after reaching its maximum value */
    public boolean isCycle() {
        return cycle;
    }

    /** @return most recently generated value */
    public long getLastValue() {
        return lastValue;
    }
}
