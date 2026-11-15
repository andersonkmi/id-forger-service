package org.codecraftlabs.idgenerator.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the ID Forger API service, which serves the
 * high-volume ID generation endpoint. Also scans the shared
 * {@code org.codecraftlabs.idgenerator.common} package (API versioning, CORS,
 * database credentials check).
 */
@SpringBootApplication(scanBasePackages = {
        "org.codecraftlabs.idgenerator.api",
        "org.codecraftlabs.idgenerator.common"
})
public class IdForgerApiApplication {
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(IdForgerApiApplication.class, args);
    }
}
