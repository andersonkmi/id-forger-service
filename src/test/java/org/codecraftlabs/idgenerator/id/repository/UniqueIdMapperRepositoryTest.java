package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.series.SeriesSequenceMapper;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueIdMapperRepositoryTest {

    @Mock
    private UniqueIdMapper uniqueIdMapper;

    @Mock
    private SeriesSequenceMapper seriesSequenceMapper;

    @InjectMocks
    private UniqueIdMapperRepository repository;

    @Test
    void should_return_next_id_when_series_exists() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(uniqueIdMapper.getNextId("default_sequence")).thenReturn(42L);

        assertThat(repository.getNextId("default")).isEqualTo(42L);
    }

    @Test
    void getNextId_should_throw_sequence_not_found_when_series_unknown() {
        when(seriesSequenceMapper.getSequenceBySeriesName("unknown")).thenReturn(Optional.empty());

        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> repository.getNextId("unknown"))
                .withMessage("Invalid sequence name");
    }

    @Test
    void getNextId_should_throw_database_exception_on_data_access_error() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(uniqueIdMapper.getNextId("default_sequence")).thenThrow(new QueryTimeoutException("timeout"));

        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getNextId("default"))
                .withMessage("Failed to get the next sequence value");
    }

    @Test
    void should_return_current_id_when_series_exists() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(uniqueIdMapper.getCurrentSequenceValue("default_sequence")).thenReturn(99L);

        assertThat(repository.getCurrentId("default")).isEqualTo(99L);
    }

    @Test
    void getCurrentId_should_throw_sequence_not_found_when_series_unknown() {
        when(seriesSequenceMapper.getSequenceBySeriesName("unknown")).thenReturn(Optional.empty());

        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> repository.getCurrentId("unknown"))
                .withMessage("Invalid sequence name");
    }

    @Test
    void getCurrentId_should_throw_database_exception_on_data_access_error() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(uniqueIdMapper.getCurrentSequenceValue("default_sequence")).thenThrow(new QueryTimeoutException("timeout"));

        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> repository.getCurrentId("default"))
                .withMessage("Failed to get the current sequence value");
    }

    @Test
    void should_return_sequence_details() {
        Sequence expected = new Sequence("public", "default_sequence", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(uniqueIdMapper.getSequenceDetails("public", "default_sequence")).thenReturn(expected);

        assertThat(repository.getSequenceDetails("public", "default")).isSameAs(expected);
    }

    @Test
    void getSequenceDetails_should_throw_sequence_not_found_when_series_unknown() {
        when(seriesSequenceMapper.getSequenceBySeriesName("unknown")).thenReturn(Optional.empty());

        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> repository.getSequenceDetails("public", "unknown"))
                .withMessage("Invalid sequence name");
    }

    @Test
    void should_update_sequence_last_value() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        repository.updateSequenceLastValue("default", 500L);
        verify(uniqueIdMapper).updateSequenceLastValue("default_sequence", 500L);
    }

    @Test
    void updateSequenceLastValue_should_throw_update_failed_exception_on_database_error() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        doThrow(new QueryTimeoutException("db error")).when(uniqueIdMapper).updateSequenceLastValue(anyString(), anyLong());

        assertThatExceptionOfType(SequenceLastValueUpdateFailedException.class)
                .isThrownBy(() -> repository.updateSequenceLastValue("default", 500L))
                .withMessage("Failed to update sequence last value");
    }
}
