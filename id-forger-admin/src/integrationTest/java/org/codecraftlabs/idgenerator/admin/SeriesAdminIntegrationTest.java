package org.codecraftlabs.idgenerator.admin;

import org.assertj.core.api.Assertions;
import org.codecraftlabs.idgenerator.admin.controller.Series;
import org.codecraftlabs.idgenerator.common.web.IdResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import java.util.Map;

/**
 * Boots the ID Forger Admin service against a Postgres Testcontainer seeded with the shared schema.
 *
 * <p>ID generation belongs to the API service, which is not part of this module. Tests that
 * need to consume IDs advance the series' sequence directly in the database, with the same
 * statement the API uses, and verify the effect through the admin endpoints.</p>
 */
@Tag("integration")
@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(classes = IdForgerAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SeriesAdminIntegrationTest {
    @Container
    @ServiceConnection
    @SuppressWarnings({"resource", "unused"})
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.5")
            .withDatabaseName("idgenerator")
            .withUsername("idgen")
            .withPassword("idgen")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(System.getProperty("integrationTest.initSql", "../docker/pgsql/init.sql")),
                    "/docker-entrypoint-initdb.d/init.sql");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    public void testGetSequenceDetails() {
        SequenceDetails response = getDetails("default");
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
    public void testGetProductSequenceDetails() {
        SequenceDetails response = getDetails("product");
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
    public void testCurrentValueMatchesLastGenerated() {
        long id = nextId("default");
        Assertions.assertThat(getId("/idgenerator/v1/series/default/currentValue")).isEqualTo(String.valueOf(id));
    }

    @Test
    public void testCurrentValueDoesNotAdvanceSequence() {
        nextId("default");
        String first = getId("/idgenerator/v1/series/default/currentValue");
        String second = getId("/idgenerator/v1/series/default/currentValue");
        Assertions.assertThat(second).isEqualTo(first);
    }

    @Test
    public void testCurrentValueInvalidSeriesReturns404() {
        assertGetFails("/idgenerator/v1/series/fake/currentValue", "404 Not Found");
    }

    @Test
    public void testCurrentValueOfUnusedSeriesReturns400() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "NEW"));
        assertGetFails("/idgenerator/v1/series/" + name + "/currentValue", "400 Bad Request");
    }

    @Test
    public void testPatchLastValueResetsSequence() {
        IdResponse response = patchLastValue("product", 1451500000L);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.id()).isEqualTo("1451500000");
        Assertions.assertThat(nextId("product")).isEqualTo(1451500001L);
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
        long before = nextId("default");

        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.patch()
                        .uri("/idgenerator/v1/series/default")
                        .body(Map.of())
                        .retrieve()
                        .body(IdResponse.class))
                .withMessageContaining("400 Bad Request");

        Assertions.assertThat(nextId("default")).isGreaterThan(before);
    }

    @Test
    public void testPatchInvalidSeriesReturns404() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> patchLastValue("fake", 10L))
                .withMessageContaining("404 Not Found");
    }

    @Test
    public void testCreateSeriesCreatesBackingSequence() {
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
        Assertions.assertThat(prefixOf(name)).containsExactly("NEW");

        Assertions.assertThat(nextId(name)).isEqualTo(500L);
        Assertions.assertThat(nextId(name)).isEqualTo(505L);
        Assertions.assertThat(getId("/idgenerator/v1/series/" + name + "/currentValue")).isEqualTo("505");

        SequenceDetails details = getDetails(name);
        Assertions.assertThat(details).isNotNull();
        Assertions.assertThat(details.name()).isEqualTo("idfs_" + name);
        Assertions.assertThat(details.startValue()).isEqualTo(500L);
        Assertions.assertThat(details.maxValue()).isEqualTo(10000L);
        Assertions.assertThat(details.lastValue()).isEqualTo(505L);
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
        Assertions.assertThat(nextId(name)).isEqualTo(1L);
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
    public void testCreateSeriesWithoutJsonContentTypeReturns415() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> RestClient.create("http://localhost:" + port).post()
                        .uri("/idgenerator/v1/series")
                        .header("Content-Type", "text/plain")
                        .body("{\"name\":\"" + uniqueSeriesName() + "\",\"prefix\":\"TXT\"}")
                        .retrieve()
                        .body(String.class))
                .withMessageContaining("415 Unsupported Media Type");
    }

    @Test
    public void testCreateSeriesTwiceReturns409() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "DUP"));

        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> createSeries(Map.of("name", name, "prefix", "DUP")))
                .withMessageContaining("409 Conflict");
        Assertions.assertThat(nextId(name)).isEqualTo(1L);
    }

    @Test
    public void testCreatingSeededSeriesReturns409() {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> createSeries(Map.of("name", "default", "prefix", "STD")))
                .withMessageContaining("409 Conflict");
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
        nextId(name);

        ResponseEntity<Void> response = deleteSeries(name);
        Assertions.assertThat(response.getStatusCode().value()).isEqualTo(204);
        Assertions.assertThat(response.hasBody()).isFalse();

        assertGetFails("/idgenerator/v1/series/" + name + "/details", "404 Not Found");
        Assertions.assertThat(prefixOf(name)).isEmpty();
        Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_sequences WHERE sequencename = ?", Long.class, "idfs_" + name)).isZero();
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
        Assertions.assertThat(prefixOf(name)).containsExactly("NEW");
        Assertions.assertThat(nextId(name)).isEqualTo(200L);
    }

    @Test
    public void testAdminEndpointsSeeDeletedSeriesImmediately() {
        String name = uniqueSeriesName();
        createSeries(Map.of("name", name, "prefix", "ADM"));
        nextId(name);
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
        Assertions.assertThat(nextId(name)).isEqualTo(1L);
    }

    @Test
    public void testUnsupportedApiVersionReturns400() {
        assertGetFails("/idgenerator/v2/series/default/details", "400 Bad Request");
    }

    @Test
    public void testIdGenerationEndpointIsNotExposed() {
        assertGetFails("/idgenerator/v1/ids/default", "404 Not Found");
    }

    @Test
    public void testActuatorHealthIsNotAffectedByApiVersioning() {
        String health = restClient.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);
        Assertions.assertThat(health).contains("\"status\":\"UP\"");
    }

    /**
     * Consumes the next ID of a series with the same statement the API service uses.
     * The pool runs with auto-commit disabled, so the statement runs inside a transaction.
     */
    private long nextId(String series) {
        Long value = transactionTemplate.execute(status -> jdbcTemplate.queryForObject(
                "SELECT nextval(sequence::regclass) FROM series_details WHERE name = ?", Long.class, series));
        Assertions.assertThat(value).isNotNull();
        return value;
    }

    private List<String> prefixOf(String series) {
        return jdbcTemplate.queryForList("SELECT prefix FROM series_details WHERE name = ?", String.class, series);
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

    private SequenceDetails getDetails(String series) {
        return restClient.get()
                .uri("/idgenerator/v1/series/" + series + "/details")
                .retrieve()
                .body(SequenceDetails.class);
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
