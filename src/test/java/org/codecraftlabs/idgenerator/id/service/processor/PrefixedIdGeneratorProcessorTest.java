package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.repository.GeneratedId;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PrefixedIdGeneratorProcessorTest {
    @Mock
    private IdGenerator idGenerator;

    private PrefixedIdGeneratorProcessor prefixedIdGeneratorProcessor;

    @BeforeEach
    public void setup() {
        this.prefixedIdGeneratorProcessor = new PrefixedIdGeneratorProcessor(idGenerator);
    }

    @Test
    public void when_ok_id_should_return() {
        when(idGenerator.nextId("default")).thenReturn(new GeneratedId(100L, "STD"));
        assertThat(prefixedIdGeneratorProcessor.generateId("default")).isEqualTo("STD000000000000100");
    }

    @Test
    public void should_use_the_prefix_returned_with_the_value() {
        when(idGenerator.nextId("orders")).thenReturn(new GeneratedId(7L, "NEW"));
        assertThat(prefixedIdGeneratorProcessor.generateId("orders")).isEqualTo("NEW000000000000007");
    }

    @Test
    public void should_not_truncate_values_wider_than_fifteen_digits() {
        when(idGenerator.nextId("default")).thenReturn(new GeneratedId(1234567890123456L, "STD"));
        assertThat(prefixedIdGeneratorProcessor.generateId("default")).isEqualTo("STD1234567890123456");
    }

    @Test
    public void when_series_is_invalid_should_propagate_exception() {
        when(idGenerator.nextId("unknown")).thenThrow(InvalidSeriesException.class);
        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> prefixedIdGeneratorProcessor.generateId("unknown"));
    }

    @Test
    public void when_id_not_generated_should_propagate_exception() {
        when(idGenerator.nextId("default")).thenThrow(IdNotGeneratedException.class);
        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> prefixedIdGeneratorProcessor.generateId("default"));
    }
}
