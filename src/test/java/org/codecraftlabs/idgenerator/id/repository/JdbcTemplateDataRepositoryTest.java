package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcTemplateDataRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcTemplateDataRepository repository;

    @Test
    void should_return_next_sequence_value() {
        when(jdbcTemplate.queryForObject("SELECT NEXTVAL('my_seq')", Long.class)).thenReturn(42L);
        long result = repository.getNextSequenceValue("my_seq");
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void should_throw_database_exception_when_next_value_is_null() {
        when(jdbcTemplate.queryForObject("SELECT NEXTVAL('my_seq')", Long.class)).thenReturn(null);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getNextSequenceValue("my_seq"))
                .withMessage("Failed to retrieve next value");
    }

    @Test
    void should_wrap_data_access_exception_on_next_value() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getNextSequenceValue("my_seq"))
                .withMessage("Failed to get the next sequence value")
                .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void should_return_current_sequence_value() {
        when(jdbcTemplate.queryForObject("SELECT last_value from my_seq", Long.class)).thenReturn(10L);
        long result = repository.getCurrentSequenceValue("my_seq");
        assertThat(result).isEqualTo(10L);
    }

    @Test
    void should_throw_database_exception_when_current_value_is_null() {
        when(jdbcTemplate.queryForObject("SELECT last_value from my_seq", Long.class)).thenReturn(null);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentSequenceValue("my_seq"))
                .withMessage("Failed to retrieve the current value");
    }

    @Test
    void should_wrap_data_access_exception_on_current_value() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentSequenceValue("my_seq"))
                .withMessage("Failed to get the current sequence value")
                .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void should_return_sequence_details() {
        Sequence expected = new Sequence("public", "my_seq", 1, 1, Long.MAX_VALUE, 1, 1, false, 10);
        String sql = "select schemaname, sequencename, start_value, min_value, max_value, increment_by, cycle, cache_size, last_value from pg_sequences where schemaname = ? and sequencename = ?";
        when(jdbcTemplate.queryForObject(eq(sql), any(SequenceMapper.class), eq("public"), eq("my_seq")))
                .thenReturn(expected);
        Sequence result = repository.getSequenceDetails("public", "my_seq");
        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_wrap_data_access_exception_on_sequence_details() {
        String sql = "select schemaname, sequencename, start_value, min_value, max_value, increment_by, cycle, cache_size, last_value from pg_sequences where schemaname = ? and sequencename = ?";
        when(jdbcTemplate.queryForObject(eq(sql), any(SequenceMapper.class), eq("public"), eq("my_seq")))
                .thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getSequenceDetails("public", "my_seq"))
                .withMessage("Failed to get sequence details")
                .withCauseInstanceOf(DataAccessException.class);
    }

    @Test
    void should_execute_setval_statement() {
        repository.updateSequenceLastValue("my_seq", 500L);
        verify(jdbcTemplate).execute("select setval('my_seq', 500)");
    }

    @Test
    void should_wrap_data_access_exception_on_update() {
        doThrow(new QueryTimeoutException("timeout")).when(jdbcTemplate).execute(anyString());
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.updateSequenceLastValue("my_seq", 500L))
                .withMessage("Failed to update sequence last value")
                .withCauseInstanceOf(DataAccessException.class);
    }
}
