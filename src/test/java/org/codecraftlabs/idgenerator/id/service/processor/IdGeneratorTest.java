package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.series.SeriesInfo;
import org.codecraftlabs.idgenerator.id.series.SeriesToSequenceMapper;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdGeneratorTest {
    @Mock
    private UniqueIdMapperRepository uniqueIdRepository;

    @Mock
    private SeriesToSequenceMapper seriesToSequenceMapper;

    @InjectMocks
    private IdGenerator idGenerator;

    // --- generateId ---

    @Test
    public void should_return_sequence_value_as_string() {
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenReturn(Optional.of(new SeriesInfo("idfs_default", "STD")));
        when(uniqueIdRepository.getNextId("idfs_default")).thenReturn(42L);
        assertThat(idGenerator.generateId("default")).isEqualTo("42");
    }

    @Test
    public void when_series_not_mapped_should_throw_invalid_series_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateId("unknown"))
                .withMessage("Invalid series name provided");
        verifyNoInteractions(uniqueIdRepository);
    }

    @Test
    public void when_series_not_found_should_throw_invalid_series_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.of(new SeriesInfo("idfs_unknown", "UNK")));
        when(uniqueIdRepository.getNextId(anyString())).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateId("unknown"));
    }

    @Test
    public void when_database_error_occurs_should_throw_id_not_generated_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenReturn(Optional.of(new SeriesInfo("idfs_default", "STD")));
        when(uniqueIdRepository.getNextId(anyString())).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.generateId("default"));
    }

    // --- generateLongId ---

    @Test
    public void should_return_sequence_value_as_long_using_mapped_sequence_name() {
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenReturn(Optional.of(new SeriesInfo("idfs_default", "STD")));
        when(uniqueIdRepository.getNextId("idfs_default")).thenReturn(100L);
        assertThat(idGenerator.generateLongId("default")).isEqualTo(100L);
    }

    @Test
    public void when_unmapped_series_requested_should_throw_invalid_series_without_advancing_sequence() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateLongId("unknown"));
        verifyNoInteractions(uniqueIdRepository);
    }

    @Test
    public void when_mapped_sequence_does_not_exist_should_throw_invalid_series_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("unknown")).thenReturn(Optional.of(new SeriesInfo("idfs_unknown", "UNK")));
        when(uniqueIdRepository.getNextId("idfs_unknown")).thenThrow(new SequenceNotFoundException("not found"));
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateLongId("unknown"));
    }

    @Test
    public void when_database_fails_should_throw_id_not_generated_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenReturn(Optional.of(new SeriesInfo("idfs_default", "STD")));
        when(uniqueIdRepository.getNextId("idfs_default")).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.generateLongId("default"));
    }

    @Test
    public void when_series_lookup_fails_should_throw_id_not_generated_exception() {
        when(seriesToSequenceMapper.getSeriesDetails("default")).thenThrow(new DatabaseException("db error"));
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.generateLongId("default"));
        verifyNoInteractions(uniqueIdRepository);
    }
}
