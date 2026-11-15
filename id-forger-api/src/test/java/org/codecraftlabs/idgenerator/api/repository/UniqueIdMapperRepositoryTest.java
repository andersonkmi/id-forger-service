package org.codecraftlabs.idgenerator.api.repository;

import org.codecraftlabs.idgenerator.common.repository.DatabaseException;
import org.codecraftlabs.idgenerator.common.repository.SequenceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueIdMapperRepositoryTest {

    @Mock
    private UniqueIdMapper uniqueIdMapper;

    @InjectMocks
    private UniqueIdMapperRepository repository;

    @Test
    void should_return_next_id_and_prefix_for_series() {
        GeneratedId expected = new GeneratedId(42L, "STD");
        when(uniqueIdMapper.getNextIdForSeries("default")).thenReturn(expected);
        assertThat(repository.getNextIdForSeries("default")).isSameAs(expected);
    }

    @Test
    void should_return_null_next_id_when_series_does_not_exist() {
        when(uniqueIdMapper.getNextIdForSeries("unknown")).thenReturn(null);
        assertThat(repository.getNextIdForSeries("unknown")).isNull();
    }

    @Test
    void should_throw_sequence_not_found_when_backing_sequence_was_dropped() {
        SQLException undefined = new SQLException("relation \"idfs_orders\" does not exist", "42P01");
        when(uniqueIdMapper.getNextIdForSeries("orders"))
                .thenThrow(new BadSqlGrammarException("getNextIdForSeries", "SELECT nextval(...)", undefined));
        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> repository.getNextIdForSeries("orders"));
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_next_id() {
        when(uniqueIdMapper.getNextIdForSeries("default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getNextIdForSeries("default"))
                .withMessage("Failed to get the next sequence value");
    }
}
