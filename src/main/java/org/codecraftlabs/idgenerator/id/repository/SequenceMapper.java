package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} that maps a {@code pg_sequences} result-set row to a {@link Sequence} object.
 */
class SequenceMapper implements RowMapper<Sequence> {

    /**
     * Maps a single row from {@code pg_sequences} to a {@link Sequence}.
     *
     * @param rs     the result set positioned at the current row
     * @param rowNum the number of the current row
     * @return a fully populated {@link Sequence}
     * @throws SQLException if a column value cannot be read
     */
    @Override
    public Sequence mapRow(ResultSet rs, int rowNum) throws SQLException {
        String schema = rs.getString("schemaname");
        String sequence = rs.getString("sequencename");
        long startValue = rs.getLong("start_value");
        long minValue = rs.getLong("min_value");
        long maxValue = rs.getLong("max_value");
        long incrementBy = rs.getLong("increment_by");
        long cacheSize = rs.getLong("cache_size");
        boolean cycle = rs.getBoolean("cycle");
        long lastValue = rs.getLong("last_value");
        return new Sequence(schema, sequence, startValue, minValue, maxValue, incrementBy, cacheSize, cycle, lastValue);
    }
}
