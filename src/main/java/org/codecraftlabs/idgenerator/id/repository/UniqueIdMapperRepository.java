package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.slf4j.helpers.CheckReturnValue;
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
    /** Raised by {@code CREATE SEQUENCE} when the sequence already exists. */
    private static final String DUPLICATE_TABLE_SQL_STATE = "42P07";
    /** Raised when inserting a series name that is already registered in {@code series_details}. */
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
    /** Raised when a referenced sequence does not exist (e.g. dropped concurrently). */
    private static final String UNDEFINED_TABLE_SQL_STATE = "42P01";
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
            if (hasSqlState(exception, UNDEFINED_TABLE_SQL_STATE)) {
                throw new SequenceNotFoundException("Sequence backing series '" + seriesName + "' does not exist");
            }
            throw new DatabaseException("Failed to get the next sequence value", exception);
        }
    }

    /**
     * Looks up the backing sequence and prefix of the given series.
     *
     * @param name the series name
     * @return the series details, or {@code null} if the series does not exist
     * @throws DatabaseException if the database operation fails
     */
    @Nullable
    @CheckReturnValue
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
     * @throws SequenceNotFoundException              if the sequence no longer exists
     * @throws SequenceLastValueUpdateFailedException if the update fails
     */
    public void updateSequenceLastValue(@Nonnull String sequenceName,
                                        long lastValue) {
        try {
            this.uniqueIdMapper.updateSequenceLastValue(sequenceName, lastValue);
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, UNDEFINED_TABLE_SQL_STATE)) {
                throw new SequenceNotFoundException("Sequence '" + sequenceName + "' does not exist");
            }
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
     * @throws SequenceAlreadyExistsException if the sequence or the series name already exists
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
            uniqueIdMapper.createSequence(schema, name, startValue, incrementBy, minValue, maxValue);
            uniqueIdMapper.insertSeriesDetails(seriesName, name, prefix);
            Sequence sequence = uniqueIdMapper.getSequenceDetails(schema, name);
            if (sequence == null) {
                throw new DatabaseException("Created sequence could not be read back");
            }
            return sequence;
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, DUPLICATE_TABLE_SQL_STATE)
                    || hasSqlState(exception, UNIQUE_VIOLATION_SQL_STATE)) {
                throw new SequenceAlreadyExistsException("Series already exists", exception);
            }
            throw new DatabaseException("Failed to create sequence", exception);
        }
    }


    /**
     * Drops the database sequence backing the given series and removes the series
     * from {@code series_details}. Both steps run in a single transaction, so nothing
     * is removed if either step fails.
     *
     * @param schema       the database schema that owns the sequence
     * @param seriesName   the series name to unregister
     * @param sequenceName the name of the sequence to drop
     * @throws DatabaseException if the database operation fails
     */
    @Transactional
    public void deleteSeries(@Nonnull String schema,
                             @Nonnull String seriesName,
                             @Nonnull String sequenceName) {
        try {
            uniqueIdMapper.dropSequence(schema, sequenceName);
            uniqueIdMapper.deleteSeriesDetails(seriesName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to delete series and sequence", exception);
        }
    }

    /**
     * Checks whether the root cause of a data access failure is a {@link SQLException}
     * carrying the given SQLState.
     */
    private boolean hasSqlState(@Nonnull DataAccessException exception, @Nonnull String sqlState) {
        return exception.getMostSpecificCause() instanceof SQLException sqlException
                && sqlState.equals(sqlException.getSQLState());
    }
}
