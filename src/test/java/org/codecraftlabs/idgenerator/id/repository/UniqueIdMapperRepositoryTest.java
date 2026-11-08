package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueIdMapperRepositoryTest {

    @Mock
    private UniqueIdMapper uniqueIdMapper;

    @InjectMocks
    private UniqueIdMapperRepository repository;

    @Test
    void should_return_next_id_for_sequence() {
        when(uniqueIdMapper.getNextId("idfs_default")).thenReturn(42L);
        assertThat(repository.getNextId("idfs_default")).isEqualTo(42L);
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_next_id() {
        when(uniqueIdMapper.getNextId("idfs_default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getNextId("idfs_default"))
                .withMessage("Failed to get the next sequence value");
    }

    @Test
    void should_return_current_id_for_sequence() {
        when(uniqueIdMapper.getCurrentSequenceValue("idfs_default")).thenReturn(99L);
        assertThat(repository.getCurrentId("idfs_default")).isEqualTo(99L);
    }

    @Test
    void should_throw_database_exception_when_sequence_has_no_current_value() {
        when(uniqueIdMapper.getCurrentSequenceValue("idfs_default")).thenReturn(null);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentId("idfs_default"))
                .withMessage("Sequence 'idfs_default' has no current value (never called)");
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_current_id() {
        when(uniqueIdMapper.getCurrentSequenceValue("idfs_default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentId("idfs_default"))
                .withMessage("Failed to get the current sequence value");
    }

    @Test
    void should_return_sequence_details() {
        Sequence expected = new Sequence("public", "idfs_default", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(uniqueIdMapper.getSequenceDetails("public", "idfs_default")).thenReturn(expected);
        assertThat(repository.getSequenceDetails("public", "idfs_default")).isSameAs(expected);
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_sequence_details() {
        when(uniqueIdMapper.getSequenceDetails("public", "idfs_default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getSequenceDetails("public", "idfs_default"))
                .withMessage("Failed to retrieve sequence details");
    }

    @Test
    void should_update_sequence_last_value() {
        repository.updateSequenceLastValue("idfs_default", 500L);
        verify(uniqueIdMapper).updateSequenceLastValue("idfs_default", 500L);
    }

    @Test
    void should_throw_update_failed_exception_on_database_error() {
        doThrow(new QueryTimeoutException("db error")).when(uniqueIdMapper).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(SequenceLastValueUpdateFailedException.class)
                .isThrownBy(() -> repository.updateSequenceLastValue("idfs_default", 500L))
                .withMessage("Failed to update sequence last value");
    }

    @Test
    void should_create_sequence_and_return_its_details() {
        Sequence created = new Sequence("public", "idfs_new", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, null);
        when(uniqueIdMapper.getSequenceDetails("public", "idfs_new")).thenReturn(created);

        assertThat(repository.createSeries("public", "new", "NEW", "idfs_new", 1L, 1L, 1L, Long.MAX_VALUE)).isEqualTo(created);
        verify(uniqueIdMapper).createSeries("public", "idfs_new", 1L, 1L, 1L, Long.MAX_VALUE);
        verify(uniqueIdMapper).insertSeriesDetails("new", "idfs_new", "NEW");
    }

    @Test
    void should_create_sequence_with_optional_values_omitted() {
        when(uniqueIdMapper.getSequenceDetails("public", "idfs_new"))
                .thenReturn(new Sequence("public", "idfs_new", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, null));

        repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null);
        verify(uniqueIdMapper).createSeries("public", "idfs_new", null, null, null, null);
    }

    @Test
    void should_throw_database_exception_when_created_sequence_cannot_be_read_back() {
        when(uniqueIdMapper.getSequenceDetails("public", "idfs_new")).thenReturn(null);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Created sequence could not be read back");
    }

    @Test
    void should_throw_database_exception_on_data_access_error_when_reading_back_created_sequence() {
        when(uniqueIdMapper.getSequenceDetails("public", "idfs_new")).thenThrow(new QueryTimeoutException("db error"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Failed to create sequence");
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_create_sequence() {
        doThrow(new QueryTimeoutException("db error")).when(uniqueIdMapper)
                .createSeries(anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong());
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", 1L, 1L, 1L, Long.MAX_VALUE))
                .withMessage("Failed to create sequence");
    }

    @Test
    void should_throw_database_exception_when_series_details_insert_fails() {
        doThrow(new QueryTimeoutException("db error")).when(uniqueIdMapper)
                .insertSeriesDetails("new", "idfs_new", "NEW");
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Failed to create sequence");
        verify(uniqueIdMapper, never()).getSequenceDetails(anyString(), anyString());
    }

    @Test
    void should_throw_sequence_already_exists_exception_when_sequence_exists() {
        SQLException duplicate = new SQLException("relation \"idfs_new\" already exists", "42P07");
        doThrow(new BadSqlGrammarException("createSequence", "CREATE SEQUENCE public.idfs_new", duplicate))
                .when(uniqueIdMapper).createSeries("public", "idfs_new", null, null, null, null);
        assertThatExceptionOfType(SequenceAlreadyExistsException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Sequence already exists");
    }
}
