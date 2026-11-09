package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.series.SeriesInfo;
import org.codecraftlabs.idgenerator.id.series.SeriesToSequenceMapper;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

import java.util.Optional;

import static java.lang.String.valueOf;

/**
 * Core ID source shared by all {@link IdFormatProcessor} implementations.
 *
 * <p>Validates the series through {@link SeriesToSequenceMapper} (served from an
 * in-memory cache backed by the {@code series_details} table), advances the backing
 * database sequence through {@link UniqueIdMapperRepository}, and translates
 * repository exceptions into service-layer exceptions.</p>
 *
 * <p>Stateless apart from its collaborators and safe for concurrent use; uniqueness
 * of the generated values is guaranteed by the database sequence.</p>
 */
@Service
public class IdGenerator {
    private final UniqueIdMapperRepository uniqueIdMapperRepository;
    private final SeriesToSequenceMapper seriesToSequenceMapper;

    /**
     * Creates the generator with its collaborators.
     *
     * @param uniqueIdMapperRepository repository used to advance the database sequence
     * @param seriesToSequenceMapper   mapper used to check that a series is configured
     *                                 and resolve its backing sequence
     */
    @Autowired
    public IdGenerator(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository,
                       @Nonnull SeriesToSequenceMapper seriesToSequenceMapper) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
        this.seriesToSequenceMapper = seriesToSequenceMapper;
    }

    /**
     * Generates the next ID for the given series and returns it as a plain decimal
     * string, without padding or prefix (e.g. {@code "12345"}).
     *
     * @param seriesName the series name
     * @return the next ID as a decimal string
     * @throws InvalidSeriesException  if the series is not configured in
     *         {@code series_details}, or its sequence does not exist
     * @throws IdNotGeneratedException if a database error occurs while resolving the
     *         series or advancing its sequence
     * @see #generateLongId(String)
     */
    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        long value = generateLongId(seriesName);
        return valueOf(value);
    }

    /**
     * Advances the sequence backing the given series and returns the raw {@code long}
     * value. Package-private for format processors in this package that need to apply
     * numeric formatting or arithmetic to the ID.
     *
     * <p>The series is validated before the sequence is advanced, so an unknown series
     * never consumes a sequence value. Each call consumes exactly one value; values
     * consumed by failed or abandoned requests are not reused, so gaps may occur.</p>
     *
     * @param seriesName the series name
     * @return the next sequence value
     * @throws InvalidSeriesException  if the series is not configured in
     *         {@code series_details}, or its sequence does not exist
     * @throws IdNotGeneratedException if a database error occurs while resolving the
     *         series or advancing its sequence
     */
    long generateLongId(@Nonnull String seriesName) {
        try {
            Optional<SeriesInfo> seriesInfo = seriesToSequenceMapper.getSeriesDetails(seriesName);
            if (seriesInfo.isEmpty()) {
                throw new InvalidSeriesException("Invalid series name provided");
            }
            return uniqueIdMapperRepository.getNextId(seriesInfo.get().sequenceName());
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }
}
