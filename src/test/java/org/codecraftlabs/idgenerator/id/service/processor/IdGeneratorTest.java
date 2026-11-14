package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.GeneratedId;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
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
        when(uniqueIdRepository.getNextIdForSeries("default")).thenReturn(new GeneratedId(42L, "STD"));
        assertThat(idGenerator.generateId("default")).isEqualTo("42");
    }

    @Test
    public void when_series_does_not_exist_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getNextIdForSeries("unknown")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateId("unknown"))
                .withMessage("Invalid series name provided");
    }

    @Test
    public void when_sequence_was_dropped_should_throw_invalid_series_exception() {
        SequenceNotFoundException cause = new SequenceNotFoundException("not found");
        when(uniqueIdRepository.getNextIdForSeries("orders")).thenThrow(cause);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.generateId("orders"))
                .withCause(cause);
    }

    @Test
    public void when_database_error_occurs_should_throw_id_not_generated_exception() {
        DatabaseException cause = new DatabaseException("db error");
        when(uniqueIdRepository.getNextIdForSeries("default")).thenThrow(cause);
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGenerator.generateId("default"))
                .withCause(cause);
    }

    // --- nextId ---

    @Test
    public void should_return_value_and_prefix_from_a_single_repository_call() {
        GeneratedId expected = new GeneratedId(100L, "STD");
        when(uniqueIdRepository.getNextIdForSeries("default")).thenReturn(expected);

        assertThat(idGenerator.nextId("default")).isEqualTo(expected);
        verify(uniqueIdRepository, times(1)).getNextIdForSeries("default");
    }

    @Test
    public void should_read_the_database_on_every_call_without_caching() {
        when(uniqueIdRepository.getNextIdForSeries("orders"))
                .thenReturn(new GeneratedId(1L, "OLD"))
                .thenReturn(new GeneratedId(1L, "NEW"));

        assertThat(idGenerator.nextId("orders").prefix()).isEqualTo("OLD");
        assertThat(idGenerator.nextId("orders").prefix()).isEqualTo("NEW");
        verify(uniqueIdRepository, times(2)).getNextIdForSeries("orders");
    }

    @Test
    public void when_series_does_not_exist_next_id_should_throw_invalid_series_exception() {
        when(uniqueIdRepository.getNextIdForSeries("unknown")).thenReturn(null);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGenerator.nextId("unknown"));
    }
}
