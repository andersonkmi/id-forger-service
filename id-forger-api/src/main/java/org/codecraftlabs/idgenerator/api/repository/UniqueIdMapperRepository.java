package org.codecraftlabs.idgenerator.api.repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.codecraftlabs.idgenerator.common.repository.DatabaseException;
import org.codecraftlabs.idgenerator.common.repository.SequenceNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import static org.codecraftlabs.idgenerator.common.repository.SqlStates.UNDEFINED_TABLE;
import static org.codecraftlabs.idgenerator.common.repository.SqlStates.hasSqlState;

/**
 * Repository component that wraps {@link UniqueIdMapper} to advance series
 * sequences with consistent exception translation.
 */
@Component
public class UniqueIdMapperRepository {
    private final UniqueIdMapper uniqueIdMapper;

    /**
     * Constructs a new {@code UniqueIdMapperRepository}.
     *
     * @param uniqueIdMapper the MyBatis mapper for sequence operations
     */
    public UniqueIdMapperRepository(@Nonnull UniqueIdMapper uniqueIdMapper) {
        this.uniqueIdMapper = uniqueIdMapper;
    }

    /**
     * Resolves the given series and advances its backing database sequence in a single
     * round trip. Reads the live {@code series_details} mapping on every call, so changes
     * made by any instance (deletes, re-creations, prefix changes) take effect immediately.
     *
     * @param seriesName the series name
     * @return the generated value and the series prefix, or {@code null} if the series does not exist
     * @throws SequenceNotFoundException if the series' backing sequence no longer exists
     *                                   (e.g. the series was deleted concurrently)
     * @throws DatabaseException         if the database operation fails
     */
    @Nullable
    public GeneratedId getNextIdForSeries(@Nonnull String seriesName) {
        try {
            return uniqueIdMapper.getNextIdForSeries(seriesName);
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, UNDEFINED_TABLE)) {
                throw new SequenceNotFoundException("Sequence backing series '" + seriesName + "' does not exist");
            }
            throw new DatabaseException("Failed to get the next sequence value", exception);
        }
    }
}
