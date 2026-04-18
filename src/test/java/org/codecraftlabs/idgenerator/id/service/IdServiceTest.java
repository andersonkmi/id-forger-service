package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.processor.IdFormatProcessor;
import org.codecraftlabs.idgenerator.id.service.processor.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdServiceTest {
    @Mock
    private IdGenerationServiceFactory idGenerationServiceFactory;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private IdFormatProcessor idFormatProcessor;

    @InjectMocks
    private IdService idService;

    @Test
    public void with_explicit_format_should_route_to_matching_processor() {
        when(idGenerationServiceFactory.getProcessor("base64")).thenReturn(Optional.of(idFormatProcessor));
        when(idFormatProcessor.generateId("default")).thenReturn("MTAw");
        assertThat(idService.generateId("default", "base64")).isEqualTo("MTAw");
    }

    @Test
    public void with_null_format_should_fall_back_to_plain_processor() {
        when(idGenerationServiceFactory.getProcessor("plain")).thenReturn(Optional.of(idFormatProcessor));
        when(idFormatProcessor.generateId("default")).thenReturn("42");
        assertThat(idService.generateId("default", null)).isEqualTo("42");
    }

    @Test
    public void with_blank_format_should_fall_back_to_plain_processor() {
        when(idGenerationServiceFactory.getProcessor("plain")).thenReturn(Optional.of(idFormatProcessor));
        when(idFormatProcessor.generateId("default")).thenReturn("42");
        assertThat(idService.generateId("default", "   ")).isEqualTo("42");
    }

    @Test
    public void with_unknown_format_should_throw_invalid_format_exception() {
        when(idGenerationServiceFactory.getProcessor("unknown")).thenReturn(Optional.empty());
        assertThatExceptionOfType(InvalidFormatException.class)
                .isThrownBy(() -> idService.generateId("default", "unknown"));
    }

    @Test
    public void should_return_current_value_from_id_generator() {
        when(idGenerator.getCurrentValue("default")).thenReturn("100");
        assertThat(idService.getCurrentValue("default")).isEqualTo("100");
    }

    @Test
    public void should_return_sequence_details_from_id_generator() {
        Sequence expected = new Sequence("public", "default_sequence", 1, 1, Long.MAX_VALUE, 1, 1, false, 10);
        when(idGenerator.getSequenceDetails("default")).thenReturn(expected);
        assertThat(idService.getSequenceDetails("default")).isSameAs(expected);
    }

    @Test
    public void should_forward_last_value_update_to_id_generator() {
        idService.updateSequenceLastValue("default", 500L);
        verify(idGenerator).updateSequenceLastValue("default", 500L);
    }
}
