package org.codecraftlabs.idgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the ID Generator service.
 */
@SpringBootApplication
public class MainApplication {
    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
