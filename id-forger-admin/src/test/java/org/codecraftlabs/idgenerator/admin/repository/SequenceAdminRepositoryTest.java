package org.codecraftlabs.idgenerator.admin.repository;

import org.codecraftlabs.idgenerator.admin.service.SequenceLastValueUpdateFailedException;
import org.codecraftlabs.idgenerator.common.repository.DatabaseException;
import org.codecraftlabs.idgenerator.common.repository.SequenceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenceAdminRepositoryTest {

    @Mock
    private SequenceAdminMapper sequenceAdminMapper;

    @InjectMocks
    private SequenceAdminRepository repository;

    @Test
    void should_return_series_details() {
        SeriesDetail expected = new SeriesDetail("idfs_default", "STD");
        when(sequenceAdminMapper.getSeriesDetails("default")).thenReturn(expected);
        assertThat(repository.getSeriesDetails("default")).isSameAs(expected);
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_series_details() {
        when(sequenceAdminMapper.getSeriesDetails("default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getSeriesDetails("default"))
                .withMessage("Failed to get series details");
    }

    @Test
    void should_return_current_id_for_sequence() {
        when(sequenceAdminMapper.getCurrentSequenceValue("idfs_default")).thenReturn(99L);
        assertThat(repository.getCurrentId("idfs_default")).isEqualTo(99L);
    }

    @Test
    void should_throw_database_exception_when_sequence_has_no_current_value() {
        when(sequenceAdminMapper.getCurrentSequenceValue("idfs_default")).thenReturn(null);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentId("idfs_default"))
                .withMessage("Sequence 'idfs_default' has no current value (never called)");
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_current_id() {
        when(sequenceAdminMapper.getCurrentSequenceValue("idfs_default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentId("idfs_default"))
                .withMessage("Failed to get the current sequence value");
    }

    @Test
    void should_return_sequence_details() {
        Sequence expected = new Sequence("public", "idfs_default", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(sequenceAdminMapper.getSequenceDetails("public", "idfs_default")).thenReturn(expected);
        assertThat(repository.getSequenceDetails("public", "idfs_default")).isSameAs(expected);
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_sequence_details() {
        when(sequenceAdminMapper.getSequenceDetails("public", "idfs_default")).thenThrow(new QueryTimeoutException("timeout"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getSequenceDetails("public", "idfs_default"))
                .withMessage("Failed to retrieve sequence details");
    }

    @Test
    void should_update_sequence_last_value() {
        repository.updateSequenceLastValue("idfs_default", 500L);
        verify(sequenceAdminMapper).updateSequenceLastValue("idfs_default", 500L);
    }

    @Test
    void should_throw_update_failed_exception_on_database_error() {
        doThrow(new QueryTimeoutException("db error")).when(sequenceAdminMapper).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(SequenceLastValueUpdateFailedException.class)
                .isThrownBy(() -> repository.updateSequenceLastValue("idfs_default", 500L))
                .withMessage("Failed to update sequence last value");
    }

    @Test
    void should_throw_sequence_not_found_when_updating_a_dropped_sequence() {
        SQLException undefined = new SQLException("relation \"idfs_orders\" does not exist", "42P01");
        doThrow(new BadSqlGrammarException("updateSequenceLastValue", "SELECT setval(...)", undefined))
                .when(sequenceAdminMapper).updateSequenceLastValue("idfs_orders", 500L);
        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> repository.updateSequenceLastValue("idfs_orders", 500L));
    }

    @Test
    void should_create_sequence_and_return_its_details() {
        Sequence created = new Sequence("public", "idfs_new", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, null);
        when(sequenceAdminMapper.getSequenceDetails("public", "idfs_new")).thenReturn(created);

        assertThat(repository.createSeries("public", "new", "NEW", "idfs_new", 1L, 1L, 1L, Long.MAX_VALUE)).isEqualTo(created);
        verify(sequenceAdminMapper).createSequence("public", "idfs_new", 1L, 1L, 1L, Long.MAX_VALUE);
        verify(sequenceAdminMapper).insertSeriesDetails("new", "idfs_new", "NEW");
    }

    @Test
    void should_create_sequence_with_optional_values_omitted() {
        when(sequenceAdminMapper.getSequenceDetails("public", "idfs_new"))
                .thenReturn(new Sequence("public", "idfs_new", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, null));

        repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null);
        verify(sequenceAdminMapper).createSequence("public", "idfs_new", null, null, null, null);
    }

    @Test
    void should_throw_database_exception_when_created_sequence_cannot_be_read_back() {
        when(sequenceAdminMapper.getSequenceDetails("public", "idfs_new")).thenReturn(null);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Created sequence could not be read back");
    }

    @Test
    void should_throw_database_exception_on_data_access_error_when_reading_back_created_sequence() {
        when(sequenceAdminMapper.getSequenceDetails("public", "idfs_new")).thenThrow(new QueryTimeoutException("db error"));
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Failed to create sequence");
    }

    @Test
    void should_throw_database_exception_on_data_access_error_for_create_sequence() {
        doThrow(new QueryTimeoutException("db error")).when(sequenceAdminMapper)
                .createSequence(anyString(), anyString(), anyLong(), anyLong(), anyLong(), anyLong());
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", 1L, 1L, 1L, Long.MAX_VALUE))
                .withMessage("Failed to create sequence");
    }

    @Test
    void should_throw_database_exception_when_series_details_insert_fails() {
        doThrow(new QueryTimeoutException("db error")).when(sequenceAdminMapper)
                .insertSeriesDetails("new", "idfs_new", "NEW");
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Failed to create sequence");
        verify(sequenceAdminMapper, never()).getSequenceDetails(anyString(), anyString());
    }

    @Test
    void should_throw_sequence_already_exists_exception_when_sequence_exists() {
        SQLException duplicate = new SQLException("relation \"idfs_new\" already exists", "42P07");
        doThrow(new BadSqlGrammarException("createSequence", "CREATE SEQUENCE public.idfs_new", duplicate))
                .when(sequenceAdminMapper).createSequence("public", "idfs_new", null, null, null, null);
        assertThatExceptionOfType(SequenceAlreadyExistsException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Series already exists");
    }

    @Test
    void should_throw_sequence_already_exists_exception_when_series_name_is_already_registered() {
        SQLException duplicateKey = new SQLException("duplicate key value violates unique constraint \"series_details_pkey\"", "23505");
        doThrow(new DuplicateKeyException("insertSeriesDetails", duplicateKey))
                .when(sequenceAdminMapper).insertSeriesDetails("new", "idfs_new", "NEW");
        assertThatExceptionOfType(SequenceAlreadyExistsException.class)
                .isThrownBy(() -> repository.createSeries("public", "new", "NEW", "idfs_new", null, null, null, null))
                .withMessage("Series already exists");
    }

    @Test
    void should_drop_sequence_and_delete_series_details() {
        repository.deleteSeries("public", "new", "idfs_new");
        InOrder inOrder = inOrder(sequenceAdminMapper);
        inOrder.verify(sequenceAdminMapper).dropSequence("public", "idfs_new");
        inOrder.verify(sequenceAdminMapper).deleteSeriesDetails("new");
    }

    @Test
    void should_throw_database_exception_when_drop_sequence_fails() {
        doThrow(new QueryTimeoutException("db error")).when(sequenceAdminMapper).dropSequence("public", "idfs_new");
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.deleteSeries("public", "new", "idfs_new"))
                .withMessage("Failed to delete series and sequence");
        verify(sequenceAdminMapper, never()).deleteSeriesDetails(anyString());
    }

    @Test
    void should_throw_database_exception_when_delete_series_details_fails() {
        doThrow(new QueryTimeoutException("db error")).when(sequenceAdminMapper).deleteSeriesDetails("new");
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.deleteSeries("public", "new", "idfs_new"))
                .withMessage("Failed to delete series and sequence");
    }
}
