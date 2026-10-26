package org.codecraftlabs.idgenerator.controller.admin;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.Bulkhead;
import org.codecraftlabs.idgenerator.controller.IdResponse;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.service.SequenceAdminService;
import org.codecraftlabs.idgenerator.id.service.SequenceDetailsRetrievalException;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SequenceAdminControllerTest {
    @Mock
    private SequenceAdminService sequenceAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        SequenceAdminController controller = new SequenceAdminController(sequenceAdminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void should_return_current_value_for_known_series() throws Exception {
        when(sequenceAdminService.getCurrentValue("default")).thenReturn("100");

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("100"));
    }

    @Test
    public void should_return_404_when_current_value_series_is_invalid() throws Exception {
        when(sequenceAdminService.getCurrentValue("unknown"))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_return_400_when_current_value_lookup_fails() throws Exception {
        when(sequenceAdminService.getCurrentValue("default"))
                .thenThrow(new IdNotGeneratedException("Failed to get current value", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_sequence_details_for_known_series() throws Exception {
        SequenceDataResponse details = new SequenceDataResponse("public", "default_sequence",
                1L, 1L, Long.MAX_VALUE, 1L, 1L, false, 10L);
        when(sequenceAdminService.getSequenceDetails("default")).thenReturn(details);

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema").value("public"))
                .andExpect(jsonPath("$.name").value("default_sequence"))
                .andExpect(jsonPath("$.lastValue").value(10));
    }

    @Test
    public void should_return_404_when_details_series_is_invalid() throws Exception {
        when(sequenceAdminService.getSequenceDetails("unknown"))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_return_400_when_details_retrieval_fails() throws Exception {
        when(sequenceAdminService.getSequenceDetails("default"))
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

        verify(sequenceAdminService).updateSequenceLastValue("default", 500L);
    }

    @Test
    public void should_return_503_when_admin_bulkhead_is_full() {
        Bulkhead bulkhead = Bulkhead.of("admin",
                BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(java.time.Duration.ZERO).build());
        bulkhead.tryAcquirePermission();

        BulkheadFullException ex = BulkheadFullException.createBulkheadFullException(bulkhead);

        var response = new SequenceAdminController(sequenceAdminService).handleBulkheadFull(ex);

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(503);
        org.assertj.core.api.Assertions.assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
    }

    @Test
    public void should_return_400_when_last_value_update_fails() throws Exception {
        doThrow(new SequenceLastValueUpdateFailedException("nope", new RuntimeException()))
                .when(sequenceAdminService).updateSequenceLastValue("default", 500L);

        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastValue\":500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_404_when_last_value_update_series_is_invalid() throws Exception {
        doThrow(new InvalidSeriesException("Invalid series name provided"))
                .when(sequenceAdminService).updateSequenceLastValue("unknown", 500L);

        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastValue\":500}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void should_include_reason_when_last_value_update_fails() throws Exception {
        doThrow(new SequenceLastValueUpdateFailedException("nope", new RuntimeException()))
                .when(sequenceAdminService).updateSequenceLastValue("default", 500L);

        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastValue\":500}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(reasonOf(result.getResolvedException()))
                        .isEqualTo("Could not update the last value"));
    }

    @Test
    public void should_return_json_content_type_when_updating_last_value() throws Exception {
        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastValue\":1451000500}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1451000500"));

        verify(sequenceAdminService).updateSequenceLastValue("product", 1451000500L);
    }

    @Test
    public void should_default_last_value_to_zero_when_body_omits_field() throws Exception {
        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("0"));

        verify(sequenceAdminService).updateSequenceLastValue("default", 0L);
    }

    @Test
    public void should_return_400_and_not_call_service_when_body_is_malformed() throws Exception {
        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sequenceAdminService);
    }

    @Test
    public void should_return_400_and_not_call_service_when_body_is_missing() throws Exception {
        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sequenceAdminService);
    }

    @Test
    public void should_return_415_when_update_content_type_is_not_json() throws Exception {
        mockMvc.perform(patch("/idgenerator/v1/ids/{seriesName}", "default")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("500"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(sequenceAdminService);
    }

    @Test
    public void should_wrap_update_failure_as_cause_when_invoked_directly() {
        SequenceLastValueUpdateFailedException cause =
                new SequenceLastValueUpdateFailedException("nope", new RuntimeException());
        doThrow(cause).when(sequenceAdminService).updateSequenceLastValue("default", 10L);
        SeriesLastValue body = new SeriesLastValue();
        body.setNewLastValue(10L);

        assertThatThrownBy(() -> new SequenceAdminController(sequenceAdminService).changeLastValue("default", body))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(cause)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    public void should_return_ok_response_entity_when_updating_directly() {
        SeriesLastValue body = new SeriesLastValue();
        body.setNewLastValue(77L);

        ResponseEntity<IdResponse> response = new SequenceAdminController(sequenceAdminService).changeLastValue("default", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new IdResponse("77"));
        verify(sequenceAdminService).updateSequenceLastValue("default", 77L);
    }

    @Test
    public void should_include_reason_when_current_value_series_is_invalid() throws Exception {
        when(sequenceAdminService.getCurrentValue("unknown"))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(reasonOf(result.getResolvedException()))
                        .isEqualTo("Series name is invalid"));
    }

    @Test
    public void should_include_reason_when_current_value_lookup_fails() throws Exception {
        when(sequenceAdminService.getCurrentValue("default"))
                .thenThrow(new IdNotGeneratedException("Failed to get current value", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(reasonOf(result.getResolvedException()))
                        .isEqualTo("Could not retrieve current value"));
    }

    @Test
    public void should_wrap_current_value_exceptions_as_cause() {
        InvalidSeriesException invalidSeries = new InvalidSeriesException("Invalid series name provided");
        IdNotGeneratedException notGenerated = new IdNotGeneratedException("boom", new RuntimeException());
        when(sequenceAdminService.getCurrentValue("unknown")).thenThrow(invalidSeries);
        when(sequenceAdminService.getCurrentValue("default")).thenThrow(notGenerated);
        SequenceAdminController controller = new SequenceAdminController(sequenceAdminService);

        assertThatThrownBy(() -> controller.getCurrentId("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(invalidSeries);
        assertThatThrownBy(() -> controller.getCurrentId("default"))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(notGenerated);
    }

    @Test
    public void should_return_all_sequence_detail_fields() throws Exception {
        SequenceDataResponse details = new SequenceDataResponse("public", "product_sequence",
                1451000000L, 1451000000L, 1451999999L, 1L, 1L, false, 1451000010L);
        when(sequenceAdminService.getSequenceDetails("product")).thenReturn(details);

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "product"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.schema").value("public"))
                .andExpect(jsonPath("$.name").value("product_sequence"))
                .andExpect(jsonPath("$.startValue").value(1451000000L))
                .andExpect(jsonPath("$.minValue").value(1451000000L))
                .andExpect(jsonPath("$.maxValue").value(1451999999L))
                .andExpect(jsonPath("$.incrementBy").value(1))
                .andExpect(jsonPath("$.cacheSize").value(1))
                .andExpect(jsonPath("$.cycle").value(false))
                .andExpect(jsonPath("$.lastValue").value(1451000010L));
    }

    @Test
    public void should_include_reason_when_details_series_is_invalid() throws Exception {
        when(sequenceAdminService.getSequenceDetails("unknown"))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(reasonOf(result.getResolvedException()))
                        .isEqualTo("Series name is invalid"));
    }

    @Test
    public void should_include_reason_when_details_retrieval_fails() throws Exception {
        when(sequenceAdminService.getSequenceDetails("default"))
                .thenThrow(new SequenceDetailsRetrievalException("Boom", new RuntimeException()));

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/details", "default"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(reasonOf(result.getResolvedException()))
                        .isEqualTo("Could not retrieve sequence details"));
    }

    @Test
    public void should_wrap_details_exceptions_as_cause() {
        InvalidSeriesException invalidSeries = new InvalidSeriesException("Invalid series name provided");
        SequenceDetailsRetrievalException retrievalFailed =
                new SequenceDetailsRetrievalException("Boom", new RuntimeException());
        when(sequenceAdminService.getSequenceDetails("unknown")).thenThrow(invalidSeries);
        when(sequenceAdminService.getSequenceDetails("default")).thenThrow(retrievalFailed);
        SequenceAdminController controller = new SequenceAdminController(sequenceAdminService);

        assertThatThrownBy(() -> controller.getSequenceDetails("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(invalidSeries);
        assertThatThrownBy(() -> controller.getSequenceDetails("default"))
                .isInstanceOf(ResponseStatusException.class)
                .hasCause(retrievalFailed);
    }

    @Test
    public void should_propagate_unexpected_exceptions() {
        IllegalStateException unexpected = new IllegalStateException("boom");
        when(sequenceAdminService.getCurrentValue("default")).thenThrow(unexpected);
        when(sequenceAdminService.getSequenceDetails("default")).thenThrow(unexpected);
        doThrow(unexpected).when(sequenceAdminService).updateSequenceLastValue("default", 1L);
        SequenceAdminController controller = new SequenceAdminController(sequenceAdminService);
        SeriesLastValue body = new SeriesLastValue();
        body.setNewLastValue(1L);

        assertThatThrownBy(() -> controller.getCurrentId("default")).isSameAs(unexpected);
        assertThatThrownBy(() -> controller.getSequenceDetails("default")).isSameAs(unexpected);
        assertThatThrownBy(() -> controller.changeLastValue("default", body)).isSameAs(unexpected);
    }

    @Test
    public void should_return_503_via_mvc_when_bulkhead_full_exception_is_raised() throws Exception {
        when(sequenceAdminService.getCurrentValue("default")).thenThrow(fullBulkheadException());

        mockMvc.perform(get("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(content().string("Admin endpoints are temporarily saturated, please retry shortly"));
    }

    @Test
    public void should_return_saturation_message_in_bulkhead_full_body() {
        ResponseEntity<String> response = new SequenceAdminController(sequenceAdminService)
                .handleBulkheadFull(fullBulkheadException());

        assertThat(response.getBody()).isEqualTo("Admin endpoints are temporarily saturated, please retry shortly");
    }

    @Test
    public void should_map_validation_errors_to_field_messages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SeriesLastValue(), "seriesLastValue");
        bindingResult.addError(new FieldError("seriesLastValue", "newLastValue", "must be positive"));
        bindingResult.addError(new FieldError("seriesLastValue", "seriesName", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
                SequenceAdminController.class.getMethod("changeLastValue", String.class, SeriesLastValue.class), 1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        Map<String, String> errors = new SequenceAdminController(sequenceAdminService).handleValidationExceptions(exception);

        assertThat(errors)
                .hasSize(2)
                .containsEntry("newLastValue", "must be positive")
                .containsEntry("seriesName", "must not be blank");
    }

    @Test
    public void should_return_empty_map_when_no_validation_errors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SeriesLastValue(), "seriesLastValue");
        MethodParameter parameter = new MethodParameter(
                SequenceAdminController.class.getMethod("changeLastValue", String.class, SeriesLastValue.class), 1);

        Map<String, String> errors = new SequenceAdminController(sequenceAdminService)
                .handleValidationExceptions(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(errors).isEmpty();
    }

    @Test
    public void should_return_405_for_unsupported_http_methods() throws Exception {
        mockMvc.perform(post("/idgenerator/v1/ids/{seriesName}/currentValue", "default"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/idgenerator/v1/ids/{seriesName}/details", "default"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(sequenceAdminService);
    }

    private static BulkheadFullException fullBulkheadException() {
        Bulkhead bulkhead = Bulkhead.of("admin",
                BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build());
        bulkhead.tryAcquirePermission();
        return BulkheadFullException.createBulkheadFullException(bulkhead);
    }

    private static String reasonOf(Exception exception) {
        return ((ResponseStatusException) exception).getReason();
    }
}
