package org.codecraftlabs.idgenerator.controller;

public record SequenceDetails(String schema, String name, long startValue, long minValue, long maxValue,
                              long incrementBy, int cacheSize, long lastValue) {
}
