package org.codecraftlabs.idgenerator.id.service;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.springframework.stereotype.Service;

import static java.lang.String.valueOf;

/**
 * Application service that exposes sequence inspection and maintenance operations
 * via {@link UniqueIdMapperRepository} and translates repository exceptions into
 * service-layer exceptions.
 */
@Service
public class SequenceAdminService {
    private static final String SCHEMA = "public";
    private final UniqueIdMapperRepository uniqueIdMapperRepository;

    public SequenceAdminService(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
    }

    public void createSeries(@Nonnull SeriesDataRequest request) {
        try {
            this.uniqueIdMapperRepository.createSequence(SCHEMA,
                    request.name(),
                    request.startingValue(),
                    request.incrementBy(),
                    request.minimumValue(),
                    request.maximumValue());
        } catch (DatabaseException exception) {
            throw new SeriesNotCreatedException(exception.getMessage(), exception);
        }
    }

    /**
     * Returns the current (last generated) value for the given series without advancing it.
     *
     * @param seriesName the series name
     * @return the current value as a string
     * @throws InvalidSeriesException  if the series is not found
     * @throws IdNotGeneratedException if the database operation fails
     */
    @Nonnull
    public String getCurrentValue(@Nonnull String seriesName) {
        try {
            return valueOf(uniqueIdMapperRepository.getCurrentId(seriesName));
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }

    /**
     * Retrieves full metadata for the sequence backing the given series.
     *
     * @param sequenceName the series name used to look up the sequence
     * @return the {@link Sequence} metadata
     * @throws InvalidSeriesException            if the series is not found
     * @throws SequenceDetailsRetrievalException if the database operation fails
     */
    @Nonnull
    public SequenceDataResponse getSequenceDetails(@Nonnull String sequenceName) {
        try {
            var sequence = this.uniqueIdMapperRepository.getSequenceDetails("public", sequenceName);
            return convert(sequence);
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new SequenceDetailsRetrievalException("Failed to get details about the sequence", exception);
        }
    }

    /**
     * Updates the last value of the sequence backing the given series.
     *
     * @param name      the series name
     * @param lastValue the new last value to set
     * @throws InvalidSeriesException                 if the series is not found
     * @throws SequenceLastValueUpdateFailedException if the database operation fails
     */
    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        try {
            this.uniqueIdMapperRepository.updateSequenceLastValue(name, lastValue);
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
    }

    @Nonnull
    private SequenceDataResponse convert(@Nonnull Sequence sequence) {
        return new SequenceDataResponse(sequence.schema(),
                sequence.name(),
                sequence.startValue(),
                sequence.minValue(),
                sequence.maxValue(),
                sequence.incrementBy(),
                sequence.cacheSize(),
                sequence.cycle(),
                sequence.lastValue());
    }
}
