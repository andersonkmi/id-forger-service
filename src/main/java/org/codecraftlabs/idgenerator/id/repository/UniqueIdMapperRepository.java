package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.series.SeriesToSequence;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Repository component that wraps {@link UniqueIdMapper} to provide
 * higher-level sequence operations with series-name resolution and
 * consistent exception translation.
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
     * Retrieves the next unique ID for the given series by advancing the
     * underlying database sequence.
     *
     * @param seriesName the logical series name
     * @return the next unique ID
     * @throws DatabaseException if the database operation fails
     */
    public long getNextId(@Nonnull String seriesName) {
        try {
            String sequenceName = getSequenceName(seriesName);
            return uniqueIdMapper.getNextId(sequenceName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the next sequence value", exception);
        }
    }

    /**
     * Retrieves the current ID for the given series without advancing
     * the underlying database sequence.
     *
     * @param seriesName the logical series name
     * @return the current sequence value
     * @throws DatabaseException if the database operation fails
     */
    public long getCurrentId(@Nonnull String seriesName) {
        try {
            String sequenceName = getSequenceName(seriesName);
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
     * @param name   the logical series name
     * @return a {@link Sequence} object containing the sequence metadata
     */
    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name) {
        try {
            String sequenceName = getSequenceName(name);
            return uniqueIdMapper.getSequenceDetails(schema, sequenceName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to retrieve sequence details", exception);
        }
    }

    /**
     * Updates the last value of the database sequence associated with the
     * given series name.
     *
     * @param name      the logical series name
     * @param lastValue the new last value to set
     * @throws SequenceLastValueUpdateFailedException if the update fails
     */
    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        try {
            String sequenceName = getSequenceName(name);
            this.uniqueIdMapper.updateSequenceLastValue(sequenceName, lastValue);
        } catch (DataAccessException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
    }

    /**
     * Creates a database sequence with the given name.
     *
     * @param schema      the database schema that will own the sequence
     * @param name        the name of the sequence to create
     * @param startValue  the value the sequence starts at
     * @param incrementBy the step size between consecutive values
     * @param minValue    the minimum allowed value
     * @param maxValue    the maximum allowed value
     * @param cacheSize   the number of values pre-allocated in memory
     * @param cycle       {@code true} if the sequence should wrap around after reaching its max value
     * @throws DatabaseException if the database operation fails
     */
    public void createSequence(@Nonnull String schema,
                               @Nonnull String name,
                               long startValue,
                               long incrementBy,
                               long minValue,
                               long maxValue,
                               long cacheSize,
                               boolean cycle) {
        try {
            uniqueIdMapper.createSequence(schema, name, startValue, incrementBy, minValue, maxValue, cacheSize, cycle);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to create sequence", exception);
        }
    }

    /**
     * Resolves a logical series name to its corresponding database sequence name.
     *
     * @param type the logical series name
     * @return the resolved database sequence name
     * @throws SequenceNotFoundException if no sequence is mapped to the given series name
     */
    @Nonnull
    private String getSequenceName(@Nonnull String type) {
        return SeriesToSequence.findByName(type)
                .map(SeriesToSequence::getSequenceName)
                .orElseThrow(() -> new SequenceNotFoundException("Invalid sequence name"));
    }
}
