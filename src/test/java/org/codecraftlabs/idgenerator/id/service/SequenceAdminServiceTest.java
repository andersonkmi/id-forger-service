package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceAlreadyExistsException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.SeriesDetail;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(uniqueIdRepository.createSeries("public", "orders", "ORD", "idfs_orders", 100L, 5L, 10L, 1000L))
                .thenReturn(new Sequence("public", "idfs_orders", 100L, 10L, 1000L, 5L, 1L, false, null));
        SeriesData expected = new SeriesData("orders", "ORD", 100L, 5L, 10L, 1000L);

        SeriesData request = new SeriesData("orders", "ORD", 100L, 5L, 10L, 1000L);
        assertThat(sequenceAdminService.createSeries(request)).isEqualTo(expected);
    }

    @Test
    public void should_pass_null_optional_values_to_repository() {
        when(uniqueIdRepository.createSeries("public", "orders", "ORD", "idfs_orders", null, null, null, null))
                .thenReturn(new Sequence("public", "idfs_orders", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, null));

        SeriesData request = new SeriesData("orders", "ORD", null, null, null, null);
        assertThat(sequenceAdminService.createSeries(request))
                .isEqualTo(new SeriesData("orders", "ORD", 1L, 1L, 1L, Long.MAX_VALUE));
        verify(uniqueIdRepository).createSeries("public", "orders", "ORD", "idfs_orders", null, null, null, null);
    }

    @Test
    public void when_database_error_on_create_should_throw_series_not_created_exception() {
        DatabaseException cause = new DatabaseException("Failed to create sequence");
        when(uniqueIdRepository.createSeries("public", "orders", "ORD", "idfs_orders", null, null, null, null)).thenThrow(cause);
        assertThatExceptionOfType(SeriesNotCreatedException.class)
                .isThrownBy(() -> sequenceAdminService.createSeries(
                        new SeriesData("orders", "ORD", null, null, null, null)))
                .withMessage("Failed to create sequence")
                .withCause(cause);
    }

    @Test
    public void when_series_already_exists_should_throw_series_already_exists_exception() {
        SequenceAlreadyExistsException cause = new SequenceAlreadyExistsException("Series already exists", new RuntimeException());
        when(uniqueIdRepository.createSeries("public", "orders", "ORD", "idfs_orders", null, null, null, null)).thenThrow(cause);
        assertThatExceptionOfType(SeriesAlreadyExistsException.class)
                .isThrownBy(() -> sequenceAdminService.createSeries(
                        new SeriesData("orders", "ORD", null, null, null, null)))
                .withMessage("Series already exists")
                .withCause(cause);
    }

    // --- deleteSeries ---

    @Test
    public void should_delete_series_and_its_backing_sequence() {
        mapDefaultSeries();
        sequenceAdminService.deleteSeries("default");
        verify(uniqueIdRepository).deleteSeries("public", "default", "idfs_default");
    }

    @Test
    public void when_series_does_not_exist_on_delete_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getSeriesDetails("unknown")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.deleteSeries("unknown"))
                .withMessage("Invalid series name provided");
        verify(uniqueIdRepository, never()).deleteSeries(anyString(), anyString(), anyString());
    }

    @Test
    public void when_database_error_on_series_lookup_for_delete_should_propagate_database_exception() {
        DatabaseException cause = new DatabaseException("Failed to get series details");
        when(uniqueIdRepository.getSeriesDetails("default")).thenThrow(cause);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> sequenceAdminService.deleteSeries("default"))
                .isSameAs(cause);
        verify(uniqueIdRepository, never()).deleteSeries(anyString(), anyString(), anyString());
    }

    @Test
    public void when_database_error_on_delete_should_propagate_database_exception() {
        DatabaseException cause = new DatabaseException("Failed to delete series and sequence");
        mapDefaultSeries();
        doThrow(cause).when(uniqueIdRepository).deleteSeries("public", "default", "idfs_default");
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> sequenceAdminService.deleteSeries("default"))
                .isSameAs(cause);
    }

    @Test
    public void should_read_the_series_mapping_on_every_call_without_caching() {
        when(uniqueIdRepository.getSeriesDetails("orders"))
                .thenReturn(new SeriesDetail("idfs_orders", "OLD"))
                .thenReturn(null);

        sequenceAdminService.deleteSeries("orders");
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.deleteSeries("orders"));
        verify(uniqueIdRepository, times(2)).getSeriesDetails("orders");
        verify(uniqueIdRepository, times(1)).deleteSeries("public", "orders", "idfs_orders");
    }

    // --- getCurrentValue ---

    @Test
    public void should_return_current_sequence_value_as_string() {
        mapDefaultSeries();
        when(uniqueIdRepository.getCurrentId("idfs_default")).thenReturn(55L);
        assertThat(sequenceAdminService.getCurrentValue("default")).isEqualTo("55");
    }

    @Test
    public void when_series_does_not_exist_for_current_value_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getSeriesDetails("unknown")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("unknown"));
        verify(uniqueIdRepository, never()).getCurrentId(anyString());
    }

    @Test
    public void when_series_not_found_for_current_value_should_throw_invalid_series_exception() {
        mapDefaultSeries();
        when(uniqueIdRepository.getCurrentId(anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("default"));
    }

    @Test
    public void when_database_error_on_current_value_should_throw_id_not_generated_exception() {
        mapDefaultSeries();
        when(uniqueIdRepository.getCurrentId(anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("default"));
    }

    @Test
    public void when_series_lookup_fails_on_current_value_should_throw_id_not_generated_exception() {
        when(uniqueIdRepository.getSeriesDetails("default")).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("default"));
    }

    // --- getSequenceDetails ---

    @Test
    public void should_return_sequence_details_converted_to_response() {
        mapDefaultSeries();
        Sequence sequence = new Sequence("public", "idfs_default", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        SequenceDataResponse expected = new SequenceDataResponse("public", "idfs_default", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(uniqueIdRepository.getSequenceDetails("public", "idfs_default")).thenReturn(sequence);
        assertThat(sequenceAdminService.getSequenceDetails("default")).isEqualTo(expected);
    }

    @Test
    public void when_database_error_on_details_lookup_should_throw_retrieval_exception() {
        mapDefaultSeries();
        when(uniqueIdRepository.getSequenceDetails(anyString(), anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(SequenceDetailsRetrievalException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("default"));
    }

    @Test
    public void when_series_does_not_exist_on_details_lookup_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getSeriesDetails("unknown")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("unknown"));
        verify(uniqueIdRepository, never()).getSequenceDetails(anyString(), anyString());
    }

    @Test
    public void when_series_not_found_on_details_lookup_should_throw_invalid_series_exception() {
        mapDefaultSeries();
        when(uniqueIdRepository.getSequenceDetails(anyString(), anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("default"));
    }

    @Test
    public void when_mapped_sequence_has_no_metadata_should_throw_invalid_series_exception() {
        mapDefaultSeries();
        when(uniqueIdRepository.getSequenceDetails("public", "idfs_default")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("default"));
    }

    // --- updateSequenceLastValue ---

    @Test
    public void should_update_last_value_in_repository() {
        mapDefaultSeries();
        sequenceAdminService.updateSequenceLastValue("default", 500L);
        verify(uniqueIdRepository).updateSequenceLastValue("idfs_default", 500L);
    }

    @Test
    public void when_database_error_on_update_should_throw_update_failed_exception() {
        mapDefaultSeries();
        doThrow(new DatabaseException("db error")).when(uniqueIdRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(SequenceLastValueUpdateFailedException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("default", 500L));
    }

    @Test
    public void when_series_does_not_exist_on_update_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getSeriesDetails("unknown")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("unknown", 500L));
        verify(uniqueIdRepository, never()).updateSequenceLastValue(anyString(), anyLong());
    }

    @Test
    public void when_series_not_found_on_update_should_throw_invalid_series_exception() {
        mapDefaultSeries();
        doThrow(new SequenceNotFoundException("not found")).when(uniqueIdRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("default", 500L));
    }

    private void mapDefaultSeries() {
        when(uniqueIdRepository.getSeriesDetails("default")).thenReturn(new SeriesDetail("idfs_default", "STD"));
    }
}
