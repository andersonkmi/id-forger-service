package org.codecraftlabs.idgenerator.id.service.processor;

import jakarta.annotation.Nonnull;

public interface IdFormatProcessor {
    @Nonnull
    String generateId(@Nonnull String seriesName);
}
