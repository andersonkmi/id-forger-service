package org.codecraftlabs.idgenerator.id.service;

import org.codecraftlabs.idgenerator.controller.admin.SequenceDataResponse;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SequenceAdminServiceTest {
    @Mock
    private IdSequenceService idSequenceService;

    @InjectMocks
    private SequenceAdminService sequenceAdminService;

    @Test
    public void should_return_current_value_from_id_sequence_service() {
        when(idSequenceService.getCurrentValue("default")).thenReturn("100");
        assertThat(sequenceAdminService.getCurrentValue("default")).isEqualTo("100");
    }

    @Test
    public void should_return_sequence_details_from_id_sequence_service() {
        Sequence sequence = new Sequence("public", "default_sequence", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        SequenceDataResponse expected = new SequenceDataResponse("public", "default_sequence", 1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(idSequenceService.getSequenceDetails("default")).thenReturn(sequence);
        assertThat(sequenceAdminService.getSequenceDetails("default")).isEqualTo(expected);
    }

    @Test
    public void should_forward_last_value_update_to_id_sequence_service() {
        sequenceAdminService.updateSequenceLastValue("default", 500L);
        verify(idSequenceService).updateSequenceLastValue("default", 500L);
    }
}
