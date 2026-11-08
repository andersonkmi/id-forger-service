package org.codecraftlabs.idgenerator.id.service;

public record SeriesDataRequest(String name,
                                String prefix,
                                Long startingValue,
                                Long incrementBy,
                                Long minimumValue,
                                Long maximumValue) {
}
