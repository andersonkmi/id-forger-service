package org.codecraftlabs.idgenerator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * Configures cross-origin access to every endpoint under {@code /idgenerator}.
 *
 * <p>Allowed origins are read from the {@code idgenerator.cors.allowed-origins} property
 * (a comma-separated list, e.g. {@code https://app.example.com,https://admin.example.com}).
 * When the property is empty or unset, no CORS mapping is registered and browsers block
 * cross-origin calls.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    static final String PATH_PATTERN = "/idgenerator/**";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsConfig.class);

    private final String[] allowedOrigins;

    /**
     * @param allowedOrigins origins allowed to call the API; empty disables CORS
     */
    public CorsConfig(@Value("${idgenerator.cors.allowed-origins:}") String[] allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(@Nonnull CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            LOGGER.info("No CORS origins configured, cross-origin requests are disabled");
            return;
        }
        LOGGER.info("CORS enabled for origins {}", Arrays.toString(allowedOrigins));
        registry.addMapping(PATH_PATTERN)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type",
                        "X-Amz-Date",
                        "Authorization",
                        "X-Api-Key",
                        "x-access-key",
                        "x-secret-key",
                        "x-session-key")
                .exposedHeaders("Retry-After");
    }
}
