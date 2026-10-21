package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.id.service.processor.IdFormatProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdGenerationServiceTest {
    @Mock
    private IdGenerationServiceFactory idGenerationServiceFactory;

    @Mock
    private IdFormatProcessor idFormatProcessor;

    @InjectMocks
    private IdGenerationService idGenerationService;

    @Test
    public void with_explicit_format_should_route_to_matching_processor() {
        when(idGenerationServiceFactory.getProcessor("base64")).thenReturn(Optional.of(idFormatProcessor));
        when(idFormatProcessor.generateId("default")).thenReturn("MTAw");
        assertThat(idGenerationService.generateId("default", "base64")).isEqualTo("MTAw");
    }

    @Test
    public void with_null_format_should_fall_back_to_plain_processor() {
        when(idGenerationServiceFactory.getProcessor("plain")).thenReturn(Optional.of(idFormatProcessor));
        when(idFormatProcessor.generateId("default")).thenReturn("42");
        assertThat(idGenerationService.generateId("default", null)).isEqualTo("42");
    }

    @Test
    public void with_blank_format_should_fall_back_to_plain_processor() {
        when(idGenerationServiceFactory.getProcessor("plain")).thenReturn(Optional.of(idFormatProcessor));
        when(idFormatProcessor.generateId("default")).thenReturn("42");
        assertThat(idGenerationService.generateId("default", "   ")).isEqualTo("42");
    }

    @Test
    public void with_unknown_format_should_throw_invalid_format_exception() {
        when(idGenerationServiceFactory.getProcessor("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidFormatException.class)
                .isThrownBy(() -> idGenerationService.generateId("default", "unknown"));
    }
}
