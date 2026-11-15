package org.codecraftlabs.idgenerator.admin.repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.codecraftlabs.idgenerator.admin.service.SequenceLastValueUpdateFailedException;
import org.codecraftlabs.idgenerator.common.repository.DatabaseException;
import org.codecraftlabs.idgenerator.common.repository.SequenceNotFoundException;
import org.slf4j.helpers.CheckReturnValue;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.codecraftlabs.idgenerator.common.repository.SqlStates.DUPLICATE_TABLE;
import static org.codecraftlabs.idgenerator.common.repository.SqlStates.UNDEFINED_TABLE;
import static org.codecraftlabs.idgenerator.common.repository.SqlStates.UNIQUE_VIOLATION;
import static org.codecraftlabs.idgenerator.common.repository.SqlStates.hasSqlState;

/**
 * Repository component that wraps {@link SequenceAdminMapper} to provide
 * higher-level series administration operations with consistent exception
 * translation.
 */
@Component
public class SequenceAdminRepository {
    private final SequenceAdminMapper sequenceAdminMapper;

    /**
     * Constructs a new {@code SequenceAdminRepository}.
     *
     * @param sequenceAdminMapper the MyBatis mapper for series administration operations
     */
    public SequenceAdminRepository(@Nonnull SequenceAdminMapper sequenceAdminMapper) {
        this.sequenceAdminMapper = sequenceAdminMapper;
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
            return sequenceAdminMapper.getSeriesDetails(name);
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
            Long currentValue = sequenceAdminMapper.getCurrentSequenceValue(sequenceName);
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
            return sequenceAdminMapper.getSequenceDetails(schema, sequenceName);
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
            this.sequenceAdminMapper.updateSequenceLastValue(sequenceName, lastValue);
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, UNDEFINED_TABLE)) {
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
            sequenceAdminMapper.createSequence(schema, name, startValue, incrementBy, minValue, maxValue);
            sequenceAdminMapper.insertSeriesDetails(seriesName, name, prefix);
            Sequence sequence = sequenceAdminMapper.getSequenceDetails(schema, name);
            if (sequence == null) {
                throw new DatabaseException("Created sequence could not be read back");
            }
            return sequence;
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, DUPLICATE_TABLE) || hasSqlState(exception, UNIQUE_VIOLATION)) {
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
            sequenceAdminMapper.dropSequence(schema, sequenceName);
            sequenceAdminMapper.deleteSeriesDetails(seriesName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to delete series and sequence", exception);
        }
    }
}
