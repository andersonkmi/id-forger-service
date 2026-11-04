package org.codecraftlabs.idgenerator.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Nonnull;

/**
 * Enables Spring's built-in API versioning, resolving the version from the URL path segment
 * (e.g. {@code v1} in {@code /idgenerator/v1/ids/default}). Supported versions are detected
 * from the {@code version} attribute of the controller mappings.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {
    @Override
    public void configureApiVersioning(@Nonnull ApiVersionConfigurer configurer) {
        configurer.usePathSegment(ApiPaths.VERSION_SEGMENT_INDEX);
    }
}
