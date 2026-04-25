package org.codecraftlabs.idgenerator.controller;

import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.IdService;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.service.SequenceDetailsRetrievalException;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SequenceAdminControllerTest {
    @Mock
    private IdService idService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        SequenceAdminController controller = new SequenceAdminController(idService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void should_return_current_value_for_known_series() throws Exception {
        when(idService.getCurrentValue("default")).thenReturn("100");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("100"));
    }

    @Test
    public void should_return_404_when_current_value_series_is_invalid() throws Exception {
        when(idService.getCurrentValue("unknown"))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_return_400_when_current_value_lookup_fails() throws Exception {
        when(idService.getCurrentValue("default"))
                .thenThrow(new IdNotGeneratedException("Failed to get current value", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_sequence_details_for_known_series() throws Exception {
        SequenceDataResponse details = new SequenceDataResponse("public", "default_sequence",
                1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(idService.getSequenceDetails("default")).thenReturn(details);

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema").value("public"))
                .andExpect(jsonPath("$.name").value("default_sequence"))
                .andExpect(jsonPath("$.lastValue").value(10));
    }

    @Test
    public void should_return_404_when_details_series_is_invalid() throws Exception {
        when(idService.getSequenceDetails("unknown"))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_return_400_when_details_retrieval_fails() throws Exception {
        when(idService.getSequenceDetails("default"))
                .thenThrow(new SequenceDetailsRetrievalException("Boom", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "default"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_update_last_value_and_return_new_value() throws Exception {
        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastValue\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("500"));

        verify(idService).updateSequenceLastValue("default", 500L);
    }

    @Test
    public void should_return_400_when_last_value_update_fails() throws Exception {
        doThrow(new SequenceLastValueUpdateFailedException("nope", new RuntimeException()))
                .when(idService).updateSequenceLastValue("default", 500L);

        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastValue\":500}"))
                .andExpect(status().isBadRequest());
    }
}
