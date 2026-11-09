package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceAlreadyExistsException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.series.SeriesInfo;
import org.codecraftlabs.idgenerator.id.series.SeriesToSequenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SequenceAdminServiceTest {
    @Mock
    private UniqueIdMapperRepository uniqueIdRepository;

    @Mock
    private SeriesToSequenceMapper seriesToSequenceMapper;

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
    public void when_sequence_already_exists_should_throw_series_not_created_exception() {
        SequenceAlreadyExistsException cause = new SequenceAlreadyExistsException("Sequence already exists", new RuntimeException());
        when(uniqueIdRepository.createSeries("public", "orders", "ORD", "idfs_orders", null, null, null, null)).thenThrow(cause);
        assertThatExceptionOfType(SeriesNotCreatedException.class)
                .isThrownBy(() -> sequenceAdminService.createSeries(
                        new SeriesData("orders", "ORD", null, null, null, null)))
                .withMessage("Sequence already exists")
                .withCause(cause);
    }

    // --- getCurrentValue ---

    @Test
    public void should_return_current_sequence_value_as_string() {
        mapDefaultSeries();
        when(uniqueIdRepository.getCurrentId("idfs_default")).thenReturn(55L);
        assertThat(sequenceAdminService.getCurrentValue("default")).isEqualTo("55");
    }

    @Test
    public void when_series_not_mapped_for_current_value_should_throw_invalid_series_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getCurrentValue("unknown"));
        verifyNoInteractions(uniqueIdRepository);
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
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenThrow(new DatabaseException("db error"));
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
    public void when_series_not_mapped_on_details_lookup_should_throw_invalid_series_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.getSequenceDetails("unknown"));
        verifyNoInteractions(uniqueIdRepository);
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
    public void when_series_not_mapped_on_update_should_throw_invalid_series_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("unknown", 500L));
        verifyNoInteractions(uniqueIdRepository);
    }

    @Test
    public void when_series_not_found_on_update_should_throw_invalid_series_exception() {
        mapDefaultSeries();
        doThrow(new SequenceNotFoundException("not found")).when(uniqueIdRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> sequenceAdminService.updateSequenceLastValue("default", 500L));
    }

    private void mapDefaultSeries() {
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenReturn(Optional.of(new SeriesInfo("idfs_default", "STD")));
    }
}
