package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

import static java.lang.String.valueOf;

/**
 * Service that wraps {@link UniqueIdMapperRepository} sequence query and maintenance calls
 * and translates repository exceptions into service-layer exceptions.
 */
@Service
public class IdSequenceService {
    private final UniqueIdMapperRepository uniqueIdMapperRepository;

    public IdSequenceService(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
    }

    /**
     * Returns the current last value for the given series without advancing the sequence.
     *
     * @param seriesName the series name
     * @return the current value as a decimal string
     * @throws InvalidSeriesException   if the series is not found
     * @throws IdNotGeneratedException  if the database operation fails
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
     * @param name the series name
     * @return the {@link Sequence} metadata
     * @throws SequenceDetailsRetrievalException if the database operation fails
     */
    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String name) {
        try {
            return this.uniqueIdMapperRepository.getSequenceDetails("public", name);
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
}
