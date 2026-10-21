package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

/**
 * Application service that exposes sequence inspection and maintenance operations
 * via {@link IdSequenceService}.
 */
@Service
public class SequenceAdminService {
    private final IdSequenceService idSequenceService;

    public SequenceAdminService(@Nonnull IdSequenceService idSequenceService) {
        this.idSequenceService = idSequenceService;
    }

    /**
     * Returns the current (last generated) value for the given series without advancing it.
     *
     * @param seriesName the series name
     * @return the current value as a string
     */
    @Nonnull
    public String getCurrentValue(@Nonnull String seriesName) {
        return idSequenceService.getCurrentValue(seriesName);
    }

    /**
     * Retrieves full metadata for the sequence backing the given series.
     *
     * @param sequenceName the series name used to look up the sequence
     * @return the {@link Sequence} metadata
     */
    @Nonnull
    public SequenceDataResponse getSequenceDetails(@Nonnull String sequenceName) {
        var sequence = this.idSequenceService.getSequenceDetails(sequenceName);
        return convert(sequence);
    }

    /**
     * Updates the last value of the sequence backing the given series.
     *
     * @param name      the series name
     * @param lastValue the new last value to set
     */
    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        this.idSequenceService.updateSequenceLastValue(name, lastValue);
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
