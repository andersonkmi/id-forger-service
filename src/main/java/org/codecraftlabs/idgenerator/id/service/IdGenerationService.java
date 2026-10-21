package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.id.service.processor.IdFormatProcessor;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

/**
 * Application service that orchestrates ID generation by selecting the appropriate
 * {@link IdFormatProcessor} via {@link IdGenerationServiceFactory}.
 */
@Service
public class IdGenerationService {
    private final IdGenerationServiceFactory idGenerationServiceFactory;

    public IdGenerationService(@Nonnull IdGenerationServiceFactory idGenerationServiceFactory) {
        this.idGenerationServiceFactory = idGenerationServiceFactory;
    }

    /**
     * Generates the next ID for the given series using the requested format.
     * Falls back to {@code plain} when {@code format} is null or blank.
     *
     * @param seriesName the series name
     * @param format     the output format; one of {@code plain}, {@code base64}, {@code prefixed},
     *                   {@code luhn}, {@code sha256}, {@code timestamped}
     * @return the formatted ID string
     * @throws InvalidFormatException if no processor is registered for the requested format
     */
    @Nonnull
    public String generateId(@Nonnull String seriesName,
                             @Nullable String format) {
        String type = getIdGeneratorProcessorType(format);
        Optional<IdFormatProcessor> processor = idGenerationServiceFactory.getProcessor(type);
        IdFormatProcessor idProcessor = processor.orElseThrow(() -> new InvalidFormatException("Invalid format requested"));
        return idProcessor.generateId(seriesName);
    }

    @Nonnull
    private String getIdGeneratorProcessorType(@Nullable String format) {
        return format != null && !format.isBlank() ? format : "plain";
    }
}
