package org.codecraftlabs.idgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Nonnull;

/**
 * Spring Boot entry point for the ID Generator service.
 * Configures CORS for the {@code /idgenerator} endpoint.
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

    /**
     * Registers a {@link WebMvcConfigurer} that allows cross-origin requests to
     * {@code /idgenerator} from any origin, supporting all HTTP methods and a
     * specific set of headers (Content-Type, Authorization, API key headers).
     *
     * @return the CORS configurer bean
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@Nonnull CorsRegistry registry) {
                registry.addMapping("/idgenerator")
                        .allowedOrigins("*")
                        .allowedMethods("*")
                        .allowedHeaders("Content-Type",
                                "X-Amz-Date",
                                "Authorization",
                                "authorization",
                                "X-Api-Key",
                                "x-access-key",
                                "x-secret-key",
                                "x-session-key");
            }
        };
    }
}
