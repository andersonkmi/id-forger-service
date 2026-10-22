package org.codecraftlabs.idgenerator.controller;

import org.assertj.core.api.Assertions;
import org.codecraftlabs.idgenerator.MainApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

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
}
