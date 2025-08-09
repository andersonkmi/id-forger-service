package org.codecraftlabs.idgenerator.id.processor;

import javax.annotation.Nonnull;

public interface IdFormatProcessor {
    @Nonnull
    String generateId(@Nonnull String seriesName);
}
