package org.codecraftlabs.idgenerator.controller;

import org.assertj.core.api.Assertions;
import org.codecraftlabs.idgenerator.MainApplication;
import org.codecraftlabs.idgenerator.id.util.LuhnValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Tag("integration")
@Testcontainers
@SpringBootTest(classes = MainApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IdGeneratorIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("idgenerator")
            .withUsername("idgen")
            .withPassword("idgen")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/pgsql/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    public void setup() {
        restClient = RestClient
                .builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Test
    public void testGetNewIdDefaultFormat() {
        IdResponse response = restClient.get()
                .uri("/idgenerator/v1/ids/default")
                .retrieve()
                .body(IdResponse.class);
        Assertions.assertThat(response).isNotNull();
    }

    @Test
    public void testGetSequenceDetails() {
        SequenceDetails response = restClient.get()
                .uri("/idgenerator/v1/ids/default/details")
                .retrieve()
                .body(SequenceDetails.class);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getSchema()).isEqualTo("public");
        Assertions.assertThat(response.getName()).isEqualTo("default_sequence");
        Assertions.assertThat(response.getStartValue()).isEqualTo(1L);
        Assertions.assertThat(response.getMinValue()).isEqualTo(1L);
        Assertions.assertThat(response.getMaxValue()).isEqualTo(9223372036854775807L);
        Assertions.assertThat(response.getIncrementBy()).isEqualTo(1L);
        Assertions.assertThat(response.getCacheSize()).isEqualTo(1L);
    }

    @Test
    public void testGetInvalidSeriesName() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.get()
                .uri("/idgenerator/v1/ids/fake")
                .retrieve()
                .body(IdResponse.class)).withMessageContaining("404 Not Found");

    }

    @Test
    public void testDefaultIdsAreSequential() {
        long first = Long.parseLong(getId("/idgenerator/v1/ids/default"));
        long second = Long.parseLong(getId("/idgenerator/v1/ids/default"));
        Assertions.assertThat(second).isEqualTo(first + 1);
    }

    @Test
    public void testProductIdWithinRange() {
        long id = Long.parseLong(getId("/idgenerator/v1/ids/product"));
        Assertions.assertThat(id).isBetween(1451000000L, 1451999999L);
    }

    @Test
    public void testBlankFormatFallsBackToPlain() {
        Assertions.assertThat(getId("/idgenerator/v1/ids/default?format=")).matches("\\d+");
    }

    @ParameterizedTest
    @CsvSource({
            "default,plain,\\d+",
            "default,sha256,[0-9a-f]{64}",
            "default,prefixed,STD\\d{15}",
            "product,prefixed,PROD\\d{15}",
            "default,timestamped,\\d{15}-\\d{13}"
    })
    public void testFormats(String series, String format, String regex) {
        String id = getId("/idgenerator/v1/ids/" + series + "?format=" + format);
        Assertions.assertThat(id).matches(regex);
    }

    @Test
    public void testBase64FormatDecodesToNumber() {
        String id = getId("/idgenerator/v1/ids/default?format=base64");
        String decoded = new String(Base64.getDecoder().decode(id), StandardCharsets.UTF_8);
        Assertions.assertThat(decoded).matches("\\d+");
    }

    @Test
    public void testLuhnFormatIsValid() {
        String id = getId("/idgenerator/v1/ids/default?format=luhn");
        Assertions.assertThat(id).matches("\\d+");
        Assertions.assertThat(new LuhnValidator().isValid(id)).isTrue();
    }

    @Test
    public void testTimestampedFormatContainsCurrentTimestamp() {
        long before = System.currentTimeMillis();
        String id = getId("/idgenerator/v1/ids/default?format=timestamped");
        long after = System.currentTimeMillis();
        long timestamp = Long.parseLong(id.substring(id.indexOf('-') + 1));
        Assertions.assertThat(timestamp).isBetween(before, after);
    }

    @Test
    public void testInvalidFormatReturns400() {
        assertGetFails("/idgenerator/v1/ids/default?format=xyz", "400 Bad Request");
    }

    @Test
    public void testPrefixedWithInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/ids/fake?format=prefixed", "404 Not Found");
    }

    @Test
    public void testCurrentValueMatchesLastGenerated() {
        String id = getId("/idgenerator/v1/ids/default");
        Assertions.assertThat(getId("/idgenerator/v1/ids/default/currentValue")).isEqualTo(id);
    }

    @Test
    public void testCurrentValueDoesNotAdvanceSequence() {
        getId("/idgenerator/v1/ids/default");
        String first = getId("/idgenerator/v1/ids/default/currentValue");
        String second = getId("/idgenerator/v1/ids/default/currentValue");
        Assertions.assertThat(second).isEqualTo(first);
    }

    @Test
    public void testCurrentValueInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/ids/fake/currentValue", "404 Not Found");
    }

    @Test
    public void testGetProductSequenceDetails() {
        SequenceDetails response = restClient.get()
                .uri("/idgenerator/v1/ids/product/details")
                .retrieve()
                .body(SequenceDetails.class);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getSchema()).isEqualTo("public");
        Assertions.assertThat(response.getName()).isEqualTo("product_sequence");
        Assertions.assertThat(response.getStartValue()).isEqualTo(1451000000L);
        Assertions.assertThat(response.getMinValue()).isEqualTo(1451000000L);
        Assertions.assertThat(response.getMaxValue()).isEqualTo(1451999999L);
        Assertions.assertThat(response.getIncrementBy()).isEqualTo(1L);
    }

    @Test
    public void testDetailsInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/ids/fake/details", "404 Not Found");
    }

    @Test
    public void testPatchLastValueResetsSequence() {
        IdResponse response = patchLastValue("product", 1451500000L);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.id()).isEqualTo("1451500000");
        Assertions.assertThat(getId("/idgenerator/v1/ids/product")).isEqualTo("1451500001");
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 1452000000L})
    public void testPatchOutOfRangeReturns400(long newLastValue) {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> patchLastValue("product", newLastValue))
                .withMessageContaining("400 Bad Request");
    }

    @Test
    public void testPatchInvalidSeriesReturns404() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> patchLastValue("fake", 10L))
                .withMessageContaining("404 Not Found");
    }

    private String getId(String uri) {
        IdResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(IdResponse.class);
        Assertions.assertThat(response).isNotNull();
        return response.id();
    }

    private IdResponse patchLastValue(String series, long newLastValue) {
        return restClient.patch()
                .uri("/idgenerator/v1/ids/" + series)
                .body(Map.of("newLastValue", newLastValue))
                .retrieve()
                .body(IdResponse.class);
    }

    private void assertGetFails(String uri, String expectedMessage) {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(IdResponse.class))
                .withMessageContaining(expectedMessage);
    }
}
