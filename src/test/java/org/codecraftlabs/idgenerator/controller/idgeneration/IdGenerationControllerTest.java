package org.codecraftlabs.idgenerator.controller.idgeneration;

import org.codecraftlabs.idgenerator.controller.IdResponse;
import org.codecraftlabs.idgenerator.id.service.IdGenerationService;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidFormatException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class IdGenerationControllerTest {
    @Mock
    private IdGenerationService idGenerationService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        IdGenerationController controller = new IdGenerationController(idGenerationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void should_return_generated_id_with_explicit_format() throws Exception {
        when(idGenerationService.generateId("default", "base64")).thenReturn("MTAw");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default").param("format", "base64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("MTAw"));
    }

    @Test
    public void should_return_generated_id_when_format_is_omitted() throws Exception {
        when(idGenerationService.generateId(eq("default"), eq(null))).thenReturn("42");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"));

        verify(idGenerationService).generateId("default", null);
    }

    @Test
    public void should_return_404_when_series_is_invalid() throws Exception {
        when(idGenerationService.generateId("unknown", null))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_return_400_when_format_is_invalid() throws Exception {
        when(idGenerationService.generateId("default", "weird"))
                .thenThrow(new InvalidFormatException("Invalid format requested"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default").param("format", "weird"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_400_when_id_generation_fails() throws Exception {
        when(idGenerationService.generateId("default", null))
                .thenThrow(new IdNotGeneratedException("Failed to generate id", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_json_content_type() throws Exception {
        when(idGenerationService.generateId("default", null)).thenReturn("42");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"id\":\"42\"}", JsonCompareMode.STRICT));
    }

    @Test
    public void should_pass_series_name_and_format_unchanged_to_service() throws Exception {
        when(idGenerationService.generateId("product", "timestamped")).thenReturn("000001451000000-1700000000000");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "product").param("format", "timestamped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("000001451000000-1700000000000"));

        verify(idGenerationService).generateId("product", "timestamped");
        verifyNoMoreInteractions(idGenerationService);
    }

    @Test
    public void should_pass_empty_format_to_service_as_is() throws Exception {
        when(idGenerationService.generateId("default", "")).thenReturn("42");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default").param("format", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"));

        verify(idGenerationService).generateId("default", "");
    }

    @Test
    public void should_include_reason_when_series_is_invalid() throws Exception {
        when(idGenerationService.generateId("unknown", null))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                        .isEqualTo("Series name is invalid"));
    }

    @Test
    public void should_include_reason_when_format_is_invalid() throws Exception {
        when(idGenerationService.generateId("default", "weird"))
                .thenThrow(new InvalidFormatException("Invalid format requested"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default").param("format", "weird"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                        .isEqualTo("Format is invalid"));
    }

    @Test
    public void should_include_reason_when_id_generation_fails() throws Exception {
        when(idGenerationService.generateId("default", null))
                .thenThrow(new IdNotGeneratedException("Failed to generate id", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(((ResponseStatusException) result.getResolvedException()).getReason())
                        .isEqualTo("Id not generated"));
    }

    @Test
    public void should_return_ok_response_entity_when_invoked_directly() {
        when(idGenerationService.generateId("default", "plain")).thenReturn("7");
        IdGenerationController controller = new IdGenerationController(idGenerationService);

        ResponseEntity<IdResponse> response = controller.getNextId("default", "plain");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new IdResponse("7"));
    }

    @Test
    public void should_wrap_invalid_series_exception_as_cause() {
        InvalidSeriesException cause = new InvalidSeriesException("Invalid series name provided");
        when(idGenerationService.generateId("unknown", null)).thenThrow(cause);
        IdGenerationController controller = new IdGenerationController(idGenerationService);

        assertThatThrownBy(() -> controller.getNextId("unknown", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(cause)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    public void should_wrap_invalid_format_exception_as_cause() {
        InvalidFormatException cause = new InvalidFormatException("Invalid format requested");
        when(idGenerationService.generateId("default", "weird")).thenThrow(cause);
        IdGenerationController controller = new IdGenerationController(idGenerationService);

        assertThatThrownBy(() -> controller.getNextId("default", "weird"))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(cause)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    public void should_wrap_id_not_generated_exception_as_cause() {
        IdNotGeneratedException cause = new IdNotGeneratedException("Failed to generate id", new RuntimeException());
        when(idGenerationService.generateId("default", null)).thenThrow(cause);
        IdGenerationController controller = new IdGenerationController(idGenerationService);

        assertThatThrownBy(() -> controller.getNextId("default", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(cause)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    public void should_propagate_unexpected_exceptions() {
        IllegalStateException unexpected = new IllegalStateException("boom");
        when(idGenerationService.generateId("default", null)).thenThrow(unexpected);
        IdGenerationController controller = new IdGenerationController(idGenerationService);

        assertThatThrownBy(() -> controller.getNextId("default", null))
                .isSameAs(unexpected);
    }

    @Test
    public void should_return_405_for_unsupported_http_methods() throws Exception {
        mockMvc.perform(post("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/idgenerator/v1/ids/{seriesName}", "default"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(idGenerationService);
    }
}
