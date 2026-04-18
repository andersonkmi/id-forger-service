package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdRepository;
import org.codecraftlabs.idgenerator.id.service.SequenceDetailsRetrievalException;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

import static java.lang.String.valueOf;

/**
 * Service that wraps {@link UniqueIdRepository} calls and translates repository
 * exceptions into service-layer exceptions.
 */
@Service
public class IdGenerator {
    private final UniqueIdRepository uniqueIdRepository;

    @Autowired
    public IdGenerator(UniqueIdRepository uniqueIdRepository) {
        this.uniqueIdRepository = uniqueIdRepository;
    }

    /**
     * Generates the next ID for the given series and returns it as a string.
     *
     * @param seriesName the series name
     * @return the next ID as a decimal string
     */
    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        long value = generateLongId(seriesName);
        return valueOf(value);
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
            return valueOf(uniqueIdRepository.getCurrentId(seriesName));
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }

    /**
     * Advances the sequence for the given series and returns the raw {@code long} value.
     * Used internally by format processors that need numeric arithmetic on the ID.
     *
     * @param seriesName the series name
     * @return the next sequence value
     * @throws InvalidSeriesException   if the series is not found
     * @throws IdNotGeneratedException  if the database operation fails
     */
    long generateLongId(@Nonnull String seriesName) {
        try {
            return uniqueIdRepository.getNextId(seriesName);
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
            return this.uniqueIdRepository.getSequenceDetails("public", name);
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
            this.uniqueIdRepository.updateSequenceLastValue(name, lastValue);
        } catch (DatabaseException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
    }
}
