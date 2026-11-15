package org.codecraftlabs.idgenerator.api;

import org.assertj.core.api.Assertions;
import org.codecraftlabs.idgenerator.api.util.LuhnValidator;
import org.codecraftlabs.idgenerator.common.web.IdResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Boots the ID Forger API against a Postgres Testcontainer seeded with the shared schema.
 *
 * <p>Series management belongs to the admin service, which is not part of this module.
 * Tests that need a series to be created, reset or deleted do it directly in the database,
 * the same way the admin service would, and verify that the API picks up the change
 * immediately.</p>
 */
@Tag("integration")
@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(classes = IdForgerApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IdGenerationIntegrationTest {
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
    public void testGetInvalidSeriesName() {
        assertGetFails("/idgenerator/v1/ids/fake", "404 Not Found");
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
    public void testSeriesAdministrationEndpointsAreNotExposed() {
        assertGetFails("/idgenerator/v1/series/default/details", "404 Not Found");
        assertGetFails("/idgenerator/v1/series/default/currentValue", "404 Not Found");
    }

    @Test
    public void testSeriesCreatedInDatabaseIsServedImmediately() {
        String name = uniqueSeriesName();
        createSeries(name, "NEW", "START WITH 500 INCREMENT BY 5 MINVALUE 500 MAXVALUE 10000");

        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("500");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("505");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name + "?format=prefixed")).isEqualTo("NEW000000000000510");
    }

    @Test
    public void testSeriesWithDatabaseDefaultsStartsAtOne() {
        String name = uniqueSeriesName();
        createSeries(name, "DEF", "");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("1");
    }

    @Test
    public void testResetSequenceIsHonoured() {
        String name = uniqueSeriesName();
        createSeries(name, "RST", "START WITH 1000 MINVALUE 1000 MAXVALUE 2000");
        getId("/idgenerator/v1/ids/" + name);

        sql("SELECT setval('idfs_" + name + "', 1500)");

        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name)).isEqualTo("1501");
    }

    @Test
    public void testGetIdAfterDeleteReturns404() {
        String name = uniqueSeriesName();
        createSeries(name, "DEL", "");
        getId("/idgenerator/v1/ids/" + name);

        deleteSeries(name);

        assertGetFails("/idgenerator/v1/ids/" + name, "404 Not Found");
        assertGetFails("/idgenerator/v1/ids/" + name + "?format=prefixed", "404 Not Found");
    }

    @Test
    public void testSeriesWhoseSequenceWasDroppedReturns404() {
        String name = uniqueSeriesName();
        createSeries(name, "DRP", "");
        sql("DROP SEQUENCE idfs_" + name);

        assertGetFails("/idgenerator/v1/ids/" + name, "404 Not Found");
        sql("DELETE FROM series_details WHERE name = '" + name + "'");
    }

    @Test
    public void testRecreatedSeriesUsesNewPrefix() {
        String name = uniqueSeriesName();
        createSeries(name, "OLD", "");
        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name + "?format=prefixed"))
                .isEqualTo("OLD000000000000001");

        deleteSeries(name);
        createSeries(name, "NEW", "");

        Assertions.assertThat(getId("/idgenerator/v1/ids/" + name + "?format=prefixed"))
                .isEqualTo("NEW000000000000001");
    }

    // Series names are generated by the tests (letters and digits only), so they are
    // safe to concatenate into the DDL, which does not support bind parameters
    private void createSeries(String name, String prefix, String sequenceOptions) {
        sql("CREATE SEQUENCE idfs_" + name + " " + sequenceOptions,
                "INSERT INTO series_details (name, sequence, prefix) VALUES ('" + name + "', 'idfs_" + name + "', '" + prefix + "')");
    }

    private void deleteSeries(String name) {
        sql("DROP SEQUENCE idfs_" + name,
                "DELETE FROM series_details WHERE name = '" + name + "'");
    }

    // The pool runs with auto-commit disabled, so statements must run inside a transaction to be committed
    private void sql(String... statements) {
        transactionTemplate.executeWithoutResult(status -> {
            for (String statement : statements) {
                jdbcTemplate.execute(statement);
            }
        });
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

    private void assertGetFails(String uri, String expectedMessage) {
        Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(IdResponse.class))
                .withMessageContaining(expectedMessage);
    }
}
