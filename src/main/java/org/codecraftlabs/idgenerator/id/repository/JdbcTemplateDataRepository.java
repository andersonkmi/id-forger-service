package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Nonnull;

/**
 * Low-level JDBC repository that executes SQL against PostgreSQL sequences.
 * All {@link org.springframework.dao.DataAccessException} instances are wrapped
 * in {@link DatabaseException}.
 */
@Repository
public class JdbcTemplateDataRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcTemplateDataRepository(@Nonnull JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Calls {@code NEXTVAL} on the given sequence and returns the result.
     *
     * @param sequenceName the database sequence name
     * @return the next value
     * @throws DatabaseException on any database error
     */
    long getNextSequenceValue(@Nonnull String sequenceName) {
        try {
            String statement = String.format("SELECT NEXTVAL('%s')", sequenceName);
            Long id = jdbcTemplate.queryForObject(statement, Long.class);
            if (id == null) {
                throw new DatabaseException("Failed to retrieve next value");
            }
            return id;
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the next sequence value", exception);
        }
    }

    /**
     * Reads {@code last_value} from the sequence table without advancing it.
     *
     * @param sequenceName the database sequence name
     * @return the current last value
     * @throws DatabaseException on any database error
     */
    long getCurrentSequenceValue(@Nonnull String sequenceName) {
        try {
            String statement = String.format("SELECT last_value from %s", sequenceName);
            Long id = jdbcTemplate.queryForObject(statement, Long.class);
            if (id == null) {
                throw new DatabaseException("Failed to retrieve the current value");
            }
            return id;
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the current sequence value", exception);
        }
    }

    /**
     * Queries {@code pg_sequences} to retrieve full metadata for the given sequence.
     *
     * @param schema the database schema (e.g. {@code public})
     * @param name   the sequence name
     * @return populated {@link Sequence}
     * @throws DatabaseException on any database error
     */
    @Nonnull
    Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name) {
        try {
            String statement = "select schemaname, sequencename, start_value, min_value, max_value, increment_by, cycle, cache_size, last_value from pg_sequences where schemaname = ? and sequencename = ?";
            return this.jdbcTemplate.queryForObject(statement, new SequenceMapper(), schema, name);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get sequence details", exception);
        }
    }

    /**
     * Calls {@code setval} to move the sequence to the specified value.
     *
     * @param sequenceName the database sequence name
     * @param value        the new last value
     * @throws DatabaseException on any database error
     */
    void updateSequenceLastValue(@Nonnull String sequenceName, long value) {
        try {
            String statement = String.format("select setval('%s', %d)", sequenceName, value);
            this.jdbcTemplate.execute(statement);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to update sequence last value", exception);
        }
    }
}
