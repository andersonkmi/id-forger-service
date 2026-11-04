package org.codecraftlabs.idgenerator.controller;

import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.PathApiVersionResolver;
import org.springframework.web.accept.SemanticApiVersionParser;

import java.util.List;

/**
 * Builds the same path-segment API version strategy configured by {@link ApiVersioningConfig},
 * for use with standalone MockMvc setups that do not load the Spring MVC configuration.
 */
public final class ApiVersioningTestSupport {
    private ApiVersioningTestSupport() {
    }

    public static ApiVersionStrategy pathSegmentVersionStrategy() {
        return new DefaultApiVersionStrategy(
                List.of(new PathApiVersionResolver(ApiPaths.VERSION_SEGMENT_INDEX)),
                new SemanticApiVersionParser(),
                true,
                null,
                true,
                null,
                null);
    }
}
