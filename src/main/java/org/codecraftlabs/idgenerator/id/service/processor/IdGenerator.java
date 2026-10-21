package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

import static java.lang.String.valueOf;

/**
 * Service that wraps {@link UniqueIdMapperRepository} calls and translates repository
 * exceptions into service-layer exceptions.
 */
@Service
public class IdGenerator {
    private final UniqueIdMapperRepository uniqueIdMapperRepository;

    public IdGenerator(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
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
            return uniqueIdMapperRepository.getNextId(seriesName);
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }
}
