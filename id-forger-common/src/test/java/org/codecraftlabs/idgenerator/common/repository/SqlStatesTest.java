package org.codecraftlabs.idgenerator.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SqlStatesTest {
    @Test
    void should_match_sql_state_of_the_root_cause() {
        var exception = new DataIntegrityViolationException("insert",
                new SQLException("duplicate key", SqlStates.UNIQUE_VIOLATION));

        assertThat(SqlStates.hasSqlState(exception, SqlStates.UNIQUE_VIOLATION)).isTrue();
        assertThat(SqlStates.hasSqlState(exception, SqlStates.UNDEFINED_TABLE)).isFalse();
    }

    @Test
    void should_not_match_when_root_cause_is_not_a_sql_exception() {
        assertThat(SqlStates.hasSqlState(new QueryTimeoutException("timeout"), SqlStates.UNDEFINED_TABLE)).isFalse();
    }
}
