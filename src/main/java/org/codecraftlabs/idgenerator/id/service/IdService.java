package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.controller.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.processor.IdFormatProcessor;
import org.codecraftlabs.idgenerator.id.service.processor.IdGenerator;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

/**
 * Application service that orchestrates ID generation by selecting the appropriate
 * {@link IdFormatProcessor} via {@link IdGenerationServiceFactory}.
 */
@Service
public class IdService {
    private final IdGenerationServiceFactory idGenerationServiceFactory;
    private final IdGenerator idGenerator;

    public IdService(@Nonnull IdGenerationServiceFactory idGenerationServiceFactory,
                     @Nonnull IdGenerator idGenerator) {
        this.idGenerationServiceFactory = idGenerationServiceFactory;
        this.idGenerator = idGenerator;
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

    /**
     * Returns the current (last generated) value for the given series without advancing it.
     *
     * @param seriesName the series name
     * @return the current value as a string
     */
    @Nonnull
    public String getCurrentValue(@Nonnull String seriesName) {
        return idGenerator.getCurrentValue(seriesName);
    }

    /**
     * Retrieves full metadata for the sequence backing the given series.
     *
     * @param sequenceName the series name used to look up the sequence
     * @return the {@link Sequence} metadata
     */
    @Nonnull
    public SequenceDataResponse getSequenceDetails(@Nonnull String sequenceName) {
        var sequence = this.idGenerator.getSequenceDetails(sequenceName);
        return convert(sequence);
    }

    /**
     * Updates the last value of the sequence backing the given series.
     *
     * @param name      the series name
     * @param lastValue the new last value to set
     */
    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        this.idGenerator.updateSequenceLastValue(name, lastValue);
    }

    @Nonnull
    private String getIdGeneratorProcessorType(@Nullable String format) {
        return format != null && !format.isBlank() ? format : "plain";
    }

    @Nonnull
    private SequenceDataResponse convert(@Nonnull Sequence sequence) {
        return new SequenceDataResponse(sequence.getSchema(),
                sequence.getName(),
                sequence.getStartValue(),
                sequence.getMinValue(),
                sequence.getMaxValue(),
                sequence.getIncrementBy(),
                sequence.getCacheSize(),
                sequence.isCycle(),
                sequence.getLastValue());
    }
}
