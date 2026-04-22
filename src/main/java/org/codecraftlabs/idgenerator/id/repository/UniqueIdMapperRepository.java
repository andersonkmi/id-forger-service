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
            return uniqueIdMapper.getCurrentSequenceValue(sequenceName);
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
