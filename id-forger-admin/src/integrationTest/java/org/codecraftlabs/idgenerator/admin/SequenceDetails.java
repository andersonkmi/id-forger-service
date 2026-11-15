package org.codecraftlabs.idgenerator.admin;

public record SequenceDetails(String schema, String name, long startValue, long minValue, long maxValue,
                              long incrementBy, int cacheSize, Long lastValue) {
}
