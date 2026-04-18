package org.codecraftlabs.idgenerator.id.service.processor;

import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

/**
 * {@link IdFormatProcessor} that returns the raw numeric ID as a decimal string.
 * Registered as the {@code plain} format processor.
 */
@Service("plain")
class PlainIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    PlainIdGeneratorProcessor(@Nonnull IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        return idGenerator.generateId(seriesName);
    }
}
