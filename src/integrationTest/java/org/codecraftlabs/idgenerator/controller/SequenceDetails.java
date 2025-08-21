package org.codecraftlabs.idgenerator.controller;

public class SequenceDetails {
    private String schema;
    private String name;
    private long startValue;
    private long minValue;
    private long maxValue;
    private long incrementBy;
    private int cacheSize;
    private long lastValue;

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getSchema() {
        return schema;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setStartValue(long startValue) {
        this.startValue = startValue;
    }

    public long getStartValue() {
        return startValue;
    }

    public void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    public long getMinValue() {
        return minValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setIncrementBy(long incrementBy) {
        this.incrementBy = incrementBy;
    }

    public long getIncrementBy() {
        return incrementBy;
    }

    public void setLastValue(long lastValue) {
        this.lastValue = lastValue;
    }

    public long getLastValue() {
        return lastValue;
    }
}
