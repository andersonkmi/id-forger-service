package org.codecraftlabs.idgenerator.id;

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

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    public long getStartValue() {
        return startValue;
    }

    public long getMinValue() {
        return minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public long getIncrementBy() {
        return incrementBy;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public boolean isCycle() {
        return cycle;
    }

    public long getLastValue() {
        return lastValue;
    }
}
