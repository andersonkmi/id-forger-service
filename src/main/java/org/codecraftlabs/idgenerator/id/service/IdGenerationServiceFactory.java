package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.id.service.processor.IdFormatProcessor;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Factory that holds all registered {@link IdFormatProcessor} beans, keyed by their
 * Spring bean name (the format string). Processors are injected by Spring's
 * {@code Map}-autowiring, so adding a new {@code @Service("format")} is sufficient
 * to make it available.
 */
@Component
public class IdGenerationServiceFactory {
    private final Map<String, IdFormatProcessor> processors;

    IdGenerationServiceFactory(Map<String, IdFormatProcessor> processors) {
        this.processors = processors;
    }

    /**
     * Retrieves the processor registered under the given format name.
     *
     * @param type the format name (e.g. {@code plain}, {@code base64})
     * @return the matching processor, or empty if none is registered for that type
     */
    @Nonnull
    Optional<IdFormatProcessor> getProcessor(@Nonnull String type) {
        return ofNullable(this.processors.get(type));
    }
}
