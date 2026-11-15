package org.codecraftlabs.idgenerator.common.repository;

import jakarta.annotation.Nonnull;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

/**
 * PostgreSQL SQLState codes the repositories react to, and a helper to match them
 * against a Spring {@link DataAccessException}.
 */
public final class SqlStates {
    /** Raised by {@code CREATE SEQUENCE} when the sequence already exists. */
    public static final String DUPLICATE_TABLE = "42P07";
    /** Raised when inserting a series name that is already registered in {@code series_details}. */
    public static final String UNIQUE_VIOLATION = "23505";
    /** Raised when a referenced sequence does not exist (e.g. dropped concurrently). */
    public static final String UNDEFINED_TABLE = "42P01";

    private SqlStates() {
    }

    /**
     * Checks whether the root cause of a data access failure is a {@link SQLException}
     * carrying the given SQLState.
     *
     * @param exception the data access failure
     * @param sqlState  the expected SQLState
     * @return {@code true} if the most specific cause has the given SQLState
     */
    public static boolean hasSqlState(@Nonnull DataAccessException exception, @Nonnull String sqlState) {
        return exception.getMostSpecificCause() instanceof SQLException sqlException
                && sqlState.equals(sqlException.getSQLState());
    }
}
