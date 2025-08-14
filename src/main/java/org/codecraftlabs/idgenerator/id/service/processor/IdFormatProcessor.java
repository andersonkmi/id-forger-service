package org.codecraftlabs.idgenerator.id.service.processor;

import javax.annotation.Nonnull;

public interface IdFormatProcessor {
    @Nonnull
    String generateId(@Nonnull String seriesName);
}
