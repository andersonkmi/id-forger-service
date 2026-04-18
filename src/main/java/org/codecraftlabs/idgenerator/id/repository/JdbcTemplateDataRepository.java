package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Nonnull;

@Repository
public class JdbcTemplateDataRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcTemplateDataRepository(@Nonnull JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    @Nonnull
    Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name) {
        try {
            String statement = "select schemaname, sequencename, start_value, min_value, max_value, increment_by, cycle, cache_size, last_value from pg_sequences where schemaname = ? and sequencename = ?";
            return this.jdbcTemplate.queryForObject(statement, new SequenceMapper(), schema, name);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get sequence details", exception);
        }
    }

    void updateSequenceLastValue(@Nonnull String sequenceName, long value) {
        try {
            String statement = String.format("select setval('%s', %d)", sequenceName, value);
            this.jdbcTemplate.execute(statement);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to update sequence last value", exception);
        }
    }
}
