package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TimestampedIdGeneratorProcessorTest {
    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private TimestampedIdGeneratorProcessor processor;

    @Test
    public void should_return_zero_padded_id_followed_by_current_timestamp() {
        when(idGenerator.generateLongId("default")).thenReturn(100L);
        long before = Instant.now().toEpochMilli();
        String result = processor.generateId("default");
        long after = Instant.now().toEpochMilli();

        assertThat(result).matches("\\d{15}-\\d+");
        String[] parts = result.split("-");
        assertThat(parts[0]).isEqualTo("000000000000100");
        long timestamp = Long.parseLong(parts[1]);
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    public void should_zero_pad_id_to_fifteen_digits() {
        when(idGenerator.generateLongId("default")).thenReturn(1L);
        assertThat(processor.generateId("default")).startsWith("000000000000001-");
    }

    @Test
    public void when_series_is_invalid_should_propagate_exception() {
        when(idGenerator.generateLongId(anyString())).thenThrow(InvalidSeriesException.class);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> processor.generateId("unknown"));
    }

    @Test
    public void when_id_not_generated_should_propagate_exception() {
        when(idGenerator.generateLongId(anyString())).thenThrow(IdNotGeneratedException.class);
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> processor.generateId("default"));
    }
}
