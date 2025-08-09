package org.codecraftlabs.idgenerator.id.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultIdGeneratorProcessorTest {
    @Mock
    private SimpleIdGenerator simpleIdGenerator;

    @InjectMocks
    private DefaultIdGeneratorProcessor defaultIdGeneratorProcessor;

    @Test
    public void when_sequence_not_found_should_raise_exception() {
        // Setup mock
        when(simpleIdGenerator.generateId(anyString()))
                .thenThrow(IdNotGeneratedException.class);

        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> defaultIdGeneratorProcessor.generateId(anyString()));
    }

    @Test
    public void when_database_exception_happens_should_raise_exception() {
        // Setup mock
        when(simpleIdGenerator.generateId(anyString()))
                .thenThrow(InvalidSeriesException.class);

        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> defaultIdGeneratorProcessor.generateId(anyString()));
    }

    @Test
    public void when_ok_id_should_return() {
        // Setup mock
        when(simpleIdGenerator.generateId(anyString())).thenReturn("100");
        var result = defaultIdGeneratorProcessor.generateId("default");
        assertThat(result).isEqualTo("100");
    }
}
