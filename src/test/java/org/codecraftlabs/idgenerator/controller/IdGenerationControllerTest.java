package org.codecraftlabs.idgenerator.controller;

import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.IdService;
import org.codecraftlabs.idgenerator.id.service.InvalidFormatException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class IdGenerationControllerTest {
    @Mock
    private IdService idService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        IdGenerationController controller = new IdGenerationController(idService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void should_return_generated_id_with_explicit_format() throws Exception {
        when(idService.generateId("default", "base64")).thenReturn("MTAw");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default").param("format", "base64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("MTAw"));
    }

    @Test
    public void should_return_generated_id_when_format_is_omitted() throws Exception {
        when(idService.generateId(eq("default"), eq(null))).thenReturn("42");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"));

        verify(idService).generateId("default", null);
    }

    @Test
    public void should_return_404_when_series_is_invalid() throws Exception {
        when(idService.generateId("unknown", null))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_return_400_when_format_is_invalid() throws Exception {
        when(idService.generateId("default", "weird"))
                .thenThrow(new InvalidFormatException("Invalid format requested"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default").param("format", "weird"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_400_when_id_generation_fails() throws Exception {
        when(idService.generateId("default", null))
                .thenThrow(new IdNotGeneratedException("Failed to generate id", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isBadRequest());
    }
}
