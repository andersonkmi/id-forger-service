package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceAlreadyExistsException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SequenceAdminServiceTest {
    @Mock
    private UniqueIdMapperRepository uniqueIdRepository;

    @InjectMocks
    private SequenceAdminService sequenceAdminService;

    // --- createSeries ---

    @Test
    public void should_create_sequence_in_public_schema_and_return_converted_details() {
        when(uniqueIdRepository.createSequence("public", "idfs_orders", 100L, 5L, 10L, 1000L))
                .thenReturn(new Sequence("public", "idfs_orders", 100L, 10L, 1000L, 5L, 1L, false, null));
        SeriesData expected = new SeriesData("orders", "ORD", 100L, 5L, 10L, 1000L);

        SeriesData request = new SeriesData("orders", "ORD", 100L, 5L, 10L, 1000L);
        assertThat(sequenceAdminService.createSeries(request)).isEqualTo(expected);
    }

    @Test
    public void should_pass_null_optional_values_to_repository() {
        when(uniqueIdRepository.createSequence("public", "idfs_orders", null, null, null, null))
                .thenReturn(new Sequence("public", "idfs_orders", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, null));

        SeriesData request = new SeriesData("orders", "ORD", null, null, null, null);
        assertThat(sequenceAdminService.createSeries(request))
                .isEqualTo(new SeriesData("orders", "ORD", 1L, 1L, 1L, Long.MAX_VALUE));
        verify(uniqueIdRepository).createSequence("public", "idfs_orders", null, null, null, null);
    }

    @Test
    public void when_database_error_on_create_should_throw_series_not_created_exception() {
        DatabaseException cause = new DatabaseException("Failed to create sequence");
        when(uniqueIdRepository.createSequence("public", "idfs_orders", null, null, null, null)).thenThrow(cause);
        assertThatExceptionOfType(SeriesNotCreatedException.class)
                .isThrownBy(() -> sequenceAdminService.createSeries(
                        new SeriesData("orders", "ORD", null, null, null, null)))
                .withMessage("Failed to create sequence")
                .withCause(cause);
    }

    @Test
    public void when_sequence_already_exists_should_throw_series_not_created_exception() {
        SequenceAlreadyExistsException cause = new SequenceAlreadyExistsException("Sequence already exists", new RuntimeException());
        when(uniqueIdRepository.createSequence("public", "idfs_orders", null, null, null, null)).thenThrow(cause);
        assertThatExceptionOfType(SeriesNotCreatedException.class)
                .isThrownBy(() -> sequenceAdminService.createSeries(
                        new SeriesData("orders", "ORD", null, null, null, null)))
                .withMessage("Sequence already exists")
                .withCause(cause);
    }

    // --- getCurrentValue ---

    @Test
    public void should_return_current_sequence_value_as_string() {
        when(uniqueIdRepository.getCurrentId("default")).thenReturn(55L);
        assertThat(sequenceAdminService.getCurrentValue("default")).isEqualTo("55");
    }

    @Test
    public void when_series_not_found_for_current_value_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getCurrentId(anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("unknown"));
    }

    @Test
    public void when_database_error_on_current_value_should_throw_id_not_generated_exception() {
        when(uniqueIdRepository.getCurrentId(anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("default"));
    }

    // --- getSequenceDetails ---

    @Test
    public void should_return_sequence_details_converted_to_response() {
        Sequence sequence = new Sequence("public", "default_sequence", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        SequenceDataResponse expected = new SequenceDataResponse("public", "default_sequence", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(uniqueIdRepository.getSequenceDetails("public", "default")).thenReturn(sequence);
        assertThat(sequenceAdminService.getSequenceDetails("default")).isEqualTo(expected);
    }

    @Test
    public void when_database_error_on_details_lookup_should_throw_retrieval_exception() {
        when(uniqueIdRepository.getSequenceDetails(anyString(), anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(SequenceDetailsRetrievalException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("default"));
    }

    @Test
    public void when_series_not_found_on_details_lookup_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getSequenceDetails(anyString(), anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("unknown"));
    }

    // --- updateSequenceLastValue ---

    @Test
    public void should_update_last_value_in_repository() {
        sequenceAdminService.updateSequenceLastValue("default", 500L);
        verify(uniqueIdRepository).updateSequenceLastValue("default", 500L);
    }

    @Test
    public void when_database_error_on_update_should_throw_update_failed_exception() {
        doThrow(new DatabaseException("db error")).when(uniqueIdRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(SequenceLastValueUpdateFailedException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("default", 500L));
    }

    @Test
    public void when_series_not_found_on_update_should_throw_invalid_series_exception() {
        doThrow(new SequenceNotFoundException("not found")).when(uniqueIdRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("unknown", 500L));
    }
}
