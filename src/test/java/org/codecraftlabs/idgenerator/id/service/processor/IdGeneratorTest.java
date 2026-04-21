package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.service.SequenceDetailsRetrievalException;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdGeneratorTest {
    @Mock
    private UniqueIdMapperRepository uniqueIdRepository;

    @InjectMocks
    private IdGenerator idGenerator;

    // --- generateId ---

    @Test
    public void should_return_sequence_value_as_string() {
        when(uniqueIdRepository.getNextId("default")).thenReturn(42L);
        assertThat(idGenerator.generateId("default")).isEqualTo("42");
    }

    @Test
    public void when_series_not_found_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getNextId(anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateId("unknown"));
    }

    @Test
    public void when_database_error_occurs_should_throw_id_not_generated_exception() {
        when(uniqueIdRepository.getNextId(anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.generateId("default"));
    }

    // --- generateLongId ---

    @Test
    public void should_return_sequence_value_as_long() {
        when(uniqueIdRepository.getNextId("default")).thenReturn(100L);
        assertThat(idGenerator.generateLongId("default")).isEqualTo(100L);
    }

    @Test
    public void when_unknown_series_requested_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getNextId(anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateLongId("unknown"));
    }

    @Test
    public void when_database_fails_should_throw_id_not_generated_exception() {
        when(uniqueIdRepository.getNextId(anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.generateLongId("default"));
    }

    // --- getCurrentValue ---

    @Test
    public void should_return_current_sequence_value_as_string() {
        when(uniqueIdRepository.getCurrentId("default")).thenReturn(55L);
        assertThat(idGenerator.getCurrentValue("default")).isEqualTo("55");
    }

    @Test
    public void when_series_not_found_for_current_value_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getCurrentId(anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.getCurrentValue("unknown"));
    }

    @Test
    public void when_database_error_on_current_value_should_throw_id_not_generated_exception() {
        when(uniqueIdRepository.getCurrentId(anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.getCurrentValue("default"));
    }

    // --- getSequenceDetails ---

    @Test
    public void should_return_sequence_metadata_from_repository() {
        Sequence expected = new Sequence("public", "default_sequence", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(uniqueIdRepository.getSequenceDetails(eq("public"), eq("default"))).thenReturn(expected);
        assertThat(idGenerator.getSequenceDetails("default")).isSameAs(expected);
    }

    @Test
    public void when_database_error_on_details_lookup_should_throw_retrieval_exception() {
        when(uniqueIdRepository.getSequenceDetails(anyString(), anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(SequenceDetailsRetrievalException.class)
                .isThrownBy(() -> idGenerator.getSequenceDetails("default"));
    }

    // --- updateSequenceLastValue ---

    @Test
    public void should_update_last_value_in_repository() {
        idGenerator.updateSequenceLastValue("default", 500L);
        verify(uniqueIdRepository).updateSequenceLastValue("default", 500L);
    }

    @Test
    public void when_database_error_on_update_should_throw_update_failed_exception() {
        doThrow(new DatabaseException("db error")).when(uniqueIdRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(SequenceLastValueUpdateFailedException.class)
                .isThrownBy(() -> idGenerator.updateSequenceLastValue("default", 500L));
    }
}
