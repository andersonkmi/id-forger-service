package org.codecraftlabs.idgenerator.id.service;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.SeriesDetail;
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
    private static final String SEQUENCE_NAME_PREFIX = "idfs_";

    private final UniqueIdMapperRepository uniqueIdMapperRepository;

    public SequenceAdminService(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
    }

    /**
     * Creates a new series by creating its backing database sequence in the default schema.
     * Numeric options left {@code null} in the request fall back to the database defaults.
     *
     * @param request the series definition
     * @return the metadata of the newly created sequence
     * @throws SeriesNotCreatedException if the sequence already exists or the database operation fails
     */
    @Nonnull
    public SeriesData createSeries(@Nonnull SeriesData request) {
        try {
            var sequence = this.uniqueIdMapperRepository.createSeries(SCHEMA,
                    request.name(),
                    request.prefix(),
                    addSequenceNamePrefix(request.name()),
                    request.startingValue(),
                    request.incrementBy(),
                    request.minimumValue(),
                    request.maximumValue());
            return convert(request, sequence);
        } catch (DatabaseException exception) {
            throw new SeriesNotCreatedException(exception.getMessage(), exception);
        }
    }

    /**
     * Deletes a series by dropping its backing database sequence and removing its
     * entry from the {@code series_details} table. Both operations run in a single
     * transaction, so nothing is removed if either step fails.
     *
     * @param seriesName the series name
     * @throws InvalidSeriesException if the series is not registered
     * @throws DatabaseException      if the database operation fails
     */
    public void deleteSeries(@Nonnull String seriesName) {
        this.uniqueIdMapperRepository.deleteSeries(SCHEMA, seriesName, resolveSequenceName(seriesName));
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
            return valueOf(uniqueIdMapperRepository.getCurrentId(resolveSequenceName(seriesName)));
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
            var sequence = this.uniqueIdMapperRepository.getSequenceDetails(SCHEMA, resolveSequenceName(sequenceName));
            if (sequence == null) {
                throw new InvalidSeriesException("Invalid series name provided");
            }
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
            this.uniqueIdMapperRepository.updateSequenceLastValue(resolveSequenceName(name), lastValue);
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
    }

    @Nonnull
    private SeriesData convert(@Nonnull SeriesData request,
                               @Nonnull Sequence sequence) {
        return new SeriesData(request.name(),
                request.prefix(),
                sequence.startValue(),
                sequence.incrementBy(),
                sequence.minValue(),
                sequence.maxValue());
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

    /**
     * Resolves a series name to its backing sequence name by reading the live
     * {@code series_details} mapping (nothing is cached, so the result is always current).
     *
     * @param seriesName the series name
     * @return the sequence name mapped to the series
     * @throws InvalidSeriesException if the series is not registered
     * @throws DatabaseException      if loading the mapping fails
     */
    @Nonnull
    private String resolveSequenceName(@Nonnull String seriesName) {
        SeriesDetail details = uniqueIdMapperRepository.getSeriesDetails(seriesName);
        if (details == null) {
            throw new InvalidSeriesException("Invalid series name provided");
        }
        return details.sequence();
    }

    @Nonnull
    private String addSequenceNamePrefix(@Nonnull String name) {
        return SEQUENCE_NAME_PREFIX + name;
    }
}
