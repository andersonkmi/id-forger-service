package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * Repository component that wraps {@link UniqueIdMapper} to provide
 * higher-level sequence operations with series-name resolution and
 * consistent exception translation.
 */
@Component
public class UniqueIdMapperRepository {
    private static final String DUPLICATE_TABLE_SQL_STATE = "42P07";
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
     * Retrieves the next unique ID for the given series by advancing the
     * underlying database sequence.
     *
     * @param sequenceName sequence name
     * @return the next unique ID
     * @throws DatabaseException if the database operation fails
     */
    public long getNextId(@Nonnull String sequenceName) {
        try {
            return uniqueIdMapper.getNextId(sequenceName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the next sequence value", exception);
        }
    }

    @Nonnull
    public SeriesDetail getSeriesDetails(@Nonnull String name) {
        try {
            return uniqueIdMapper.getSeriesDetails(name);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get series details", exception);
        }
    }

    /**
     * Retrieves the current ID for the given series without advancing
     * the underlying database sequence.
     *
     * @param sequenceName the sequence name
     * @return the current sequence value
     * @throws DatabaseException if the database operation fails
     */
    public long getCurrentId(@Nonnull String sequenceName) {
        try {
            Long currentValue = uniqueIdMapper.getCurrentSequenceValue(sequenceName);
            if (currentValue == null) {
                throw new DatabaseException("Sequence '" + sequenceName + "' has no current value (never called)");
            }
            return currentValue;
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the current sequence value", exception);
        }
    }

    /**
     * Retrieves the details of the database sequence associated with the
     * given series name and schema.
     *
     * @param schema the database schema containing the sequence
     * @param sequenceName  the sequence name
     * @return a {@link Sequence} object containing the sequence metadata
     */
    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String schema,
                                       @Nonnull String sequenceName) {
        try {
            return uniqueIdMapper.getSequenceDetails(schema, sequenceName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to retrieve sequence details", exception);
        }
    }

    /**
     * Updates the last value of the database sequence associated with the
     * given series name.
     *
     * @param sequenceName the sequence name
     * @param lastValue the new last value to set
     * @throws SequenceLastValueUpdateFailedException if the update fails
     */
    public void updateSequenceLastValue(@Nonnull String sequenceName,
                                        long lastValue) {
        try {
            this.uniqueIdMapper.updateSequenceLastValue(sequenceName, lastValue);
        } catch (DataAccessException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
    }

    /**
     * Creates a database sequence with the given name, registers the series in
     * {@code series_details} and returns the sequence metadata. The creation, the
     * registration and the metadata lookup run in a single transaction, so nothing
     * is persisted if any step fails.
     *
     * @param schema      the database schema that will own the sequence
     * @param seriesName  the series name to register
     * @param prefix      the prefix applied to identifiers generated for the series
     * @param name        the name of the sequence to create
     * @param startValue  the value the sequence starts at
     * @param incrementBy the step size between consecutive values
     * @param minValue    the minimum allowed value
     * @param maxValue    the maximum allowed value
     * @return the metadata of the newly created sequence
     * @throws SequenceAlreadyExistsException if a sequence with the same name already exists
     * @throws DatabaseException if the database operation fails
     */
    @Nonnull
    @Transactional
    public Sequence createSeries(@Nonnull String schema,
                                 @Nonnull String seriesName,
                                 @Nonnull String prefix,
                                 @Nonnull String name,
                                 @Nullable Long startValue,
                                 @Nullable Long incrementBy,
                                 @Nullable Long minValue,
                                 @Nullable Long maxValue) {
        try {
            uniqueIdMapper.createSeries(schema, name, startValue, incrementBy, minValue, maxValue);
            uniqueIdMapper.insertSeriesDetails(seriesName, name, prefix);
            Sequence sequence = uniqueIdMapper.getSequenceDetails(schema, name);
            if (sequence == null) {
                throw new DatabaseException("Created sequence could not be read back");
            }
            return sequence;
        } catch (DataAccessException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sqlException
                    && DUPLICATE_TABLE_SQL_STATE.equals(sqlException.getSQLState())) {
                throw new SequenceAlreadyExistsException("Sequence already exists", exception);
            }
            throw new DatabaseException("Failed to create sequence", exception);
        }
    }
}
