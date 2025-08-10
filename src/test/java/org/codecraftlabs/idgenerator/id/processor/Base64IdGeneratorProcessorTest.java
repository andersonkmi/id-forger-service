package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
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
public class Base64IdGeneratorProcessorTest {
    @Mock
    private IdManager idManager;

    @InjectMocks
    private Base64IdGeneratorProcessor base64IdGeneratorProcessor;

    @Test
    public void when_sequence_not_found_should_raise_exception() {
        // Setup mock
        when(idManager.generateId(anyString()))
                .thenThrow(IdNotGeneratedException.class);

        assertThatExceptionOfType(IdNotGeneratedException.class)
                .isThrownBy(() -> base64IdGeneratorProcessor.generateId(anyString()));
    }

    @Test
    public void when_database_exception_happens_should_raise_exception() {
        // Setup mock
        when(idManager.generateId(anyString()))
                .thenThrow(InvalidSeriesException.class);

        assertThatExceptionOfType(InvalidSeriesException.class)
                .isThrownBy(() -> base64IdGeneratorProcessor.generateId(anyString()));
    }

    @Test
    public void when_ok_id_should_return() {
        // Setup mock
        when(idManager.generateId(anyString())).thenReturn("100");
        var result = base64IdGeneratorProcessor.generateId("default");
        assertThat(result).isEqualTo("MTAw");
    }
}
