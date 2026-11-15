package org.codecraftlabs.idgenerator.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the ID Forger Admin service, which serves the
 * series management endpoints. Also scans the shared
 * {@code org.codecraftlabs.idgenerator.common} package (API versioning, CORS,
 * database credentials check).
 */
@SpringBootApplication(scanBasePackages = {
        "org.codecraftlabs.idgenerator.admin",
        "org.codecraftlabs.idgenerator.common"
})
public class IdForgerAdminApplication {
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(IdForgerAdminApplication.class, args);
    }
}
