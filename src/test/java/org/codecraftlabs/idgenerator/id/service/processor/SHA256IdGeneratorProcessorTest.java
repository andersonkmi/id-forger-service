package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SHA256IdGeneratorProcessorTest {
    @Mock
    private IdGenerator idGenerator;

    private SHA256IdGeneratorProcessor idGeneratorProcessor;

    @BeforeEach
    public void setup() {
        this.idGeneratorProcessor = new SHA256IdGeneratorProcessor(idGenerator);
    }

    @Test
    public void when_sequence_not_found_should_raise_exception() {
        // Setup mock
        when(idGenerator.generateId(anyString()))
                .thenThrow(IdNotGeneratedException.class);

        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> idGeneratorProcessor.generateId(anyString()));
    }

    @Test
    public void when_database_exception_happens_should_raise_exception() {
        // Setup mock
        when(idGenerator.generateId(anyString()))
                .thenThrow(InvalidSeriesException.class);

        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> idGeneratorProcessor.generateId(anyString()));
    }

    @Test
    public void when_ok_id_should_return() {
        // Setup mock
        when(idGenerator.generateId(anyString())).thenReturn("100");
        var result = idGeneratorProcessor.generateId("default");
        assertThat(result).isEqualTo("ad57366865126e55649ecb23ae1d48887544976efea46a48eb5d85a6eeb4d306");
    }
}
