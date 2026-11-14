package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.GeneratedId;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

import static java.lang.String.valueOf;

/**
 * Core ID source shared by all {@link IdFormatProcessor} implementations.
 *
 * <p>Resolves the series and advances its backing database sequence in a single round
 * trip through {@link UniqueIdMapperRepository}, and translates repository exceptions
 * into service-layer exceptions. Nothing is cached in memory: every call reads the live
 * {@code series_details} mapping, so several instances can share the database and
 * changes made by the admin endpoints take effect immediately.</p>
 *
 * <p>Stateless apart from its collaborator and safe for concurrent use; uniqueness
 * of the generated values is guaranteed by the database sequence.</p>
 */
@Service
public class IdGenerator {
    private final UniqueIdMapperRepository uniqueIdMapperRepository;

    /**
     * Creates the generator with its collaborator.
     *
     * @param uniqueIdMapperRepository repository used to resolve the series and advance its sequence
     */
    public IdGenerator(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
    }

    /**
     * Generates the next ID for the given series and returns it as a plain decimal
     * string, without padding or prefix (e.g. {@code "12345"}).
     *
     * @param seriesName the series name
     * @return the next ID as a decimal string
     * @throws InvalidSeriesException  if the series is not configured in
     *         {@code series_details}, or its sequence does not exist
     * @throws IdNotGeneratedException if a database error occurs
     * @see #nextId(String)
     */
    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        return valueOf(nextId(seriesName).value());
    }

    /**
     * Advances the sequence backing the given series and returns the raw value together
     * with the series prefix. Package-private for format processors in this package that
     * need the prefix or apply numeric formatting to the ID.
     *
     * <p>An unknown series never consumes a sequence value. Each successful call consumes
     * exactly one value; values consumed by failed or abandoned requests are not reused,
     * so gaps may occur.</p>
     *
     * @param seriesName the series name
     * @return the next sequence value and the series prefix
     * @throws InvalidSeriesException  if the series is not configured in
     *         {@code series_details}, or its sequence does not exist
     * @throws IdNotGeneratedException if a database error occurs
     */
    @Nonnull
    GeneratedId nextId(@Nonnull String seriesName) {
        try {
            GeneratedId generatedId = uniqueIdMapperRepository.getNextIdForSeries(seriesName);
            if (generatedId == null) {
                throw new InvalidSeriesException("Invalid series name provided");
            }
            return generatedId;
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }
}
