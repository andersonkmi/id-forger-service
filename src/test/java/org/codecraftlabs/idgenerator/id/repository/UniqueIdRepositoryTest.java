package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.series.SeriesSequenceMapper;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UniqueIdRepositoryTest {
    @Mock
    private JdbcTemplateDataRepository jdbcTemplateDataRepository;

    @Mock
    private SeriesSequenceMapper seriesSequenceMapper;

    @InjectMocks
    private UniqueIdRepository uniqueIdRepository;

    // --- getNextId ---

    @Test
    public void should_return_next_sequence_value_from_repository() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(jdbcTemplateDataRepository.getNextSequenceValue("default_sequence")).thenReturn(42L);
        assertThat(uniqueIdRepository.getNextId("default")).isEqualTo(42L);
    }

    @Test
    public void when_series_not_mapped_should_throw_sequence_not_found_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> uniqueIdRepository.getNextId("unknown"));
    }

    @Test
    public void when_jdbc_fails_on_next_value_should_propagate_database_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.of("default_sequence"));
        when(jdbcTemplateDataRepository.getNextSequenceValue(anyString())).thenThrow(DatabaseException.class);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> uniqueIdRepository.getNextId("default"));
    }

    // --- getCurrentId ---

    @Test
    public void should_return_current_sequence_value_from_repository() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(jdbcTemplateDataRepository.getCurrentSequenceValue("default_sequence")).thenReturn(10L);
        assertThat(uniqueIdRepository.getCurrentId("default")).isEqualTo(10L);
    }

    @Test
    public void when_series_not_mapped_for_current_value_should_throw_sequence_not_found_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> uniqueIdRepository.getCurrentId("unknown"));
    }

    @Test
    public void when_jdbc_fails_on_current_value_should_propagate_database_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.of("default_sequence"));
        when(jdbcTemplateDataRepository.getCurrentSequenceValue(anyString())).thenThrow(DatabaseException.class);
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> uniqueIdRepository.getCurrentId("default"));
    }

    // --- getSequenceDetails ---

    @Test
    public void should_return_sequence_details_from_repository() {
        Sequence expected = new Sequence("public", "default_sequence", 1, 1, Long.MAX_VALUE, 1, 1, false, 10);
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        when(jdbcTemplateDataRepository.getSequenceDetails("public", "default_sequence")).thenReturn(expected);
        assertThat(uniqueIdRepository.getSequenceDetails("public", "default")).isSameAs(expected);
    }

    @Test
    public void when_series_not_mapped_for_details_should_throw_sequence_not_found_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> uniqueIdRepository.getSequenceDetails("public", "unknown"));
    }

    // --- updateSequenceLastValue ---

    @Test
    public void should_resolve_series_name_and_delegate_update_to_repository() {
        when(seriesSequenceMapper.getSequenceBySeriesName("default")).thenReturn(Optional.of("default_sequence"));
        uniqueIdRepository.updateSequenceLastValue("default", 500L);
        verify(jdbcTemplateDataRepository).updateSequenceLastValue("default_sequence", 500L);
    }

    @Test
    public void when_series_not_mapped_for_update_should_throw_sequence_not_found_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.empty());
        assertThatExceptionOfType(SequenceNotFoundException.class)
                .isThrownBy(() -> uniqueIdRepository.updateSequenceLastValue("unknown", 500L));
    }

    @Test
    public void when_jdbc_fails_on_update_should_propagate_database_exception() {
        when(seriesSequenceMapper.getSequenceBySeriesName(anyString())).thenReturn(Optional.of("default_sequence"));
        doThrow(DatabaseException.class).when(jdbcTemplateDataRepository).updateSequenceLastValue(anyString(), anyLong());
        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> uniqueIdRepository.updateSequenceLastValue("default", 500L));
    }
}
