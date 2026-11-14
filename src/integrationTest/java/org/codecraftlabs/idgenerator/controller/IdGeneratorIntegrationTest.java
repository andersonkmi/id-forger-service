package org.codecraftlabs.idgenerator.controller;

import org.assertj.core.api.Assertions;
import org.codecraftlabs.idgenerator.MainApplication;
import org.codecraftlabs.idgenerator.controller.admin.Series;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Tag("integration")
@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(classes = MainApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IdGeneratorIntegrationTest {
    @Container
    @ServiceConnection
    @SuppressWarnings({"resource", "unused"})
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.5")
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
                .uri("/idgenerator/v1/series/default/details")
                .retrieve()
                .body(SequenceDetails.class);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.schema()).isEqualTo("public");
        Assertions.assertThat(response.name()).isEqualTo("idfs_default");
        Assertions.assertThat(response.startValue()).isEqualTo(1L);
        Assertions.assertThat(response.minValue()).isEqualTo(1L);
        Assertions.assertThat(response.maxValue()).isEqualTo(9223372036854775807L);
        Assertions.assertThat(response.incrementBy()).isEqualTo(1L);
        Assertions.assertThat(response.cacheSize()).isEqualTo(1L);
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
    public void testUnsupportedApiVersionReturns400() {
        assertGetFails("/idgenerator/v2/ids/default", "400 Bad Request");
    }

    @Test
    public void testActuatorHealthIsNotAffectedByApiVersioning() {
        String health = restClient.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);
        Assertions.assertThat(health).contains("\"status\":\"UP\"");
    }

    @Test
    public void testPrefixedWithInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/ids/fake?format=prefixed", "404 Not Found");
    }

    @Test
    public void testCurrentValueMatchesLastGenerated() {
        String id = getId("/idgenerator/v1/ids/default");
        Assertions.assertThat(getId("/idgenerator/v1/series/default/currentValue")).isEqualTo(id);
    }

    @Test
    public void testCurrentValueDoesNotAdvanceSequence() {
        getId("/idgenerator/v1/ids/default");
        String first = getId("/idgenerator/v1/series/default/currentValue");
        String second = getId("/idgenerator/v1/series/default/currentValue");
        Assertions.assertThat(second).isEqualTo(first);
    }

    @Test
    public void testCurrentValueInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/series/fake/currentValue", "404 Not Found");
    }

    @Test
    public void testGetProductSequenceDetails() {
        SequenceDetails response = restClient.get()
                .uri("/idgenerator/v1/series/product/details")
                .retrieve()
                .body(SequenceDetails.class);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.schema()).isEqualTo("public");
        Assertions.assertThat(response.name()).isEqualTo("idfs_product");
        Assertions.assertThat(response.startValue()).isEqualTo(1451000000L);
        Assertions.assertThat(response.minValue()).isEqualTo(1451000000L);
        Assertions.assertThat(response.maxValue()).isEqualTo(1451999999L);
        Assertions.assertThat(response.incrementBy()).isEqualTo(1L);
    }

    @Test
    public void testDetailsInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/series/fake/details", "404 Not Found");
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
    public void testPatchWithoutNewLastValueReturns400AndKeepsSequence() {
        String before = getId("/idgenerator/v1/ids/default");

        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.patch()
                        .uri("/idgenerator/v1/series/default")
                        .body(Map.of())
                        .retrieve()
                        .body(IdResponse.class))
                .withMessageContaining("400 Bad Request");

        Assertions.assertThat(Long.parseLong(getId("/idgenerator/v1/ids/default")))
                .isGreaterThan(Long.parseLong(before));
    }

    @Test
    public void testPatchInvalidSeriesReturns404() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> patchLastValue("fake", 10L))
                .withMessageContaining("404 Not Found");
    }

    @Test
    public void testCreateSeriesThenGenerateIds() {
        String name = uniqueSeriesName();
        Series created = createSeries(Map.of(
                "name", name,
                "prefix", "NEW",
                "startingValue", 500,
                "incrementBy", 5,
                "minimumValue", 500,
                "maximumValue", 10000));

        Assertions.assertThat(created).isNotNull();
        Assertions.assertThat(created.getName()).isEqualTo(name);
        Assertions.assertThat(created.getPrefix()).isEqualTo("NEW");
        Assertions.assertThat(created.getStartingValue()).isEqualTo(500L);
        Assertions.assertThat(created.getIncrementBy()).isEqualTo(5L);
        Assertions.assertThat(created.getMinimumValue()).isEqualTo(500L);
        Assertions.assertThat(created.getMaximumValue()).isEqualTo(10000L);

        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("500");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("505");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name + "?format=prefixed")).isEqualTo("NEW000000000000510");
        Assertions.assertThat(getId("/idgenerator/v1/series/" + name + "/currentValue")).isEqualTo("510");

        SequenceDetails details = restClient.get()
                .uri("/idgenerator/v1/series/" + name + "/details")
                .retrieve()
                .body(SequenceDetails.class);
        Assertions.assertThat(details).isNotNull();
        Assertions.assertThat(details.name()).isEqualTo("idfs_" + name);
        Assertions.assertThat(details.startValue()).isEqualTo(500L);
        Assertions.assertThat(details.maxValue()).isEqualTo(10000L);
    }

    @Test
    public void testCreateSeriesWithOnlyRequiredFieldsUsesDatabaseDefaults() {
        String name = uniqueSeriesName();
        Series created = createSeries(Map.of("name", name, "prefix", "DEF"));

        Assertions.assertThat(created).isNotNull();
        Assertions.assertThat(created.getName()).isEqualTo(name);
        Assertions.assertThat(created.getPrefix()).isEqualTo("DEF");
        Assertions.assertThat(created.getStartingValue()).isEqualTo(1L);
        Assertions.assertThat(created.getIncrementBy()).isEqualTo(1L);
        Assertions.assertThat(created.getMinimumValue()).isEqualTo(1L);
        Assertions.assertThat(created.getMaximumValue()).isEqualTo(Long.MAX_VALUE);
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("1");
    }

    @Test
    public void testCreateExistingSeriesReturns400() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "DUP"));

        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> createSeries(Map.of("name", name, "prefix", "DUP")))
                .withMessageContaining("400 Bad Request");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"\",\"prefix\":\"ORD\"}",
            "{\"name\":\"Orders_1\",\"prefix\":\"ORD\"}",
            "{\"name\":\"orders\"}",
            "{\"name\":\"orders\",\"prefix\":\"ABCDEFGHIJK\"}",
            "{\"name\":\"orders\",\"prefix\":\"ORD\",\"incrementBy\":0}",
            "{\"name\":\"orders\",\"prefix\":\"ORD\",\"startingValue\":-1}"
    })
    public void testCreateSeriesWithInvalidBodyReturns400(String body) {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> createSeries(body))
                .withMessageContaining("400 Bad Request");
    }

    @Test
    public void testCreateSeriesWithUnsupportedApiVersionReturns400() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.post()
                        .uri("/idgenerator/v2/series")
                        .body(Map.of("name", uniqueSeriesName(), "prefix", "VER"))
                        .retrieve()
                        .body(Series.class))
                .withMessageContaining("400 Bad Request");
    }

    @Test
    public void testDeleteSeriesRemovesSeriesAndSequence() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "DEL"));
        getId("/idgenerator/v1/ids/" + name);

        ResponseEntity<Void> response = deleteSeries(name);
        Assertions.assertThat(response.getStatusCode().value()).isEqualTo(204);
        Assertions.assertThat(response.hasBody()).isFalse();

        assertGetFails("/idgenerator/v1/series/" + name + "/details", "404 Not Found");
    }

    @Test
    public void testDeleteSeriesTwiceReturns404() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "DEL"));
        deleteSeries(name);

        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> deleteSeries(name))
                .withMessageContaining("404 Not Found");
    }

    @Test
    public void testDeletedSeriesCanBeRecreated() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "OLD", "startingValue", 100));
        deleteSeries(name);

        Series recreated = createSeries(Map.of("name", name, "prefix", "NEW", "startingValue", 200));

        Assertions.assertThat(recreated).isNotNull();
        Assertions.assertThat(recreated.getStartingValue()).isEqualTo(200L);
    }

    @Test
    public void testGetIdAfterDeleteReturns404() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "DEL"));
        getId("/idgenerator/v1/ids/" + name);

        deleteSeries(name);

        assertGetFails("/idgenerator/v1/ids/" + name, "404 Not Found");
        assertGetFails("/idgenerator/v1/ids/" + name + "?format=prefixed", "404 Not Found");
    }

    @Test
    public void testRecreatedSeriesUsesNewPrefix() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "OLD"));
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name + "?format=prefixed"))
                .isEqualTo("OLD000000000000001");

        deleteSeries(name);
        createSeries(Map.of("name", name, "prefix", "NEW"));

        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name + "?format=prefixed"))
                .isEqualTo("NEW000000000000001");
    }

    @Test
    public void testAdminEndpointsSeeDeletedSeriesImmediately() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "ADM"));
        getId("/idgenerator/v1/ids/" + name);
        Assertions.assertThat(getId("/idgenerator/v1/series/" + name + "/currentValue")).isEqualTo("1");

        deleteSeries(name);

        assertGetFails("/idgenerator/v1/series/" + name + "/currentValue", "404 Not Found");
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> patchLastValue(name, 10L))
                .withMessageContaining("404 Not Found");
    }

    @Test
    public void testDeleteInvalidSeriesReturns404() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> deleteSeries("fake"))
                .withMessageContaining("404 Not Found");
    }

    @Test
    public void testDeleteSeriesWithUnsupportedApiVersionReturns400() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "VER"));

        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.delete()
                        .uri("/idgenerator/v2/series/" + name)
                        .retrieve()
                        .body(String.class))
                .withMessageContaining("400 Bad Request");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("1");
    }

    // Uses a client without the default Content-Type header: a DELETE has no body,
    // so the endpoint must accept it without one
    private ResponseEntity<Void> deleteSeries(String series) {
        return RestClient.create("http://localhost:" + port).delete()
                .uri("/idgenerator/v1/series/" + series)
                .retrieve()
                .toBodilessEntity();
    }

    private Series createSeries(Object body) {
        return restClient.post()
                .uri("/idgenerator/v1/series")
                .body(body)
                .retrieve()
                .body(Series.class);
    }

    private static String uniqueSeriesName() {
        return "it" + System.nanoTime();
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
                .uri("/idgenerator/v1/series/" + series)
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
