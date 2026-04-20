package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import org.apache.ibatis.annotations.Mapper;
import org.codecraftlabs.idgenerator.id.Sequence;

/**
 * MyBatis mapper interface for unique ID generation operations.
 * Provides direct database access to PostgreSQL sequences for retrieving,
 * advancing, and updating sequence values.
 */
@Mapper
public interface UniqueIdMapper {
    /**
     * Retrieves the next value from the specified database sequence.
     *
     * @param seriesName the name of the database sequence
     * @return the next value in the sequence
     */
    long getNextId(@Nonnull String seriesName);
    /**
     * Retrieves the current value of the specified database sequence
     * without advancing it.
     *
     * @param seriesName the name of the database sequence
     * @return the current value of the sequence
     */
    long getCurrentSequenceValue(@Nonnull String seriesName);
    /**
     * Retrieves the details of a database sequence, including its metadata.
     *
     * @param schema the database schema containing the sequence
     * @param name   the name of the sequence
     * @return a {@link Sequence} object containing the sequence details
     */
    Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name);
    /**
     * Updates the last value of the specified database sequence.
     *
     * @param name      the name of the sequence to update
     * @param lastValue the new last value to set on the sequence
     */
    void updateSequenceLastValue(@Nonnull String name, long lastValue);
}
