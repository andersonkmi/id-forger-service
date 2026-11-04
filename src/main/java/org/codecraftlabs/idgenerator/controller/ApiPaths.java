package org.codecraftlabs.idgenerator.controller;

/**
 * Centralised base path and API versions for the public REST API.
 * The version is resolved by Spring's built-in API versioning from the path segment
 * at {@link #VERSION_SEGMENT_INDEX} (e.g. {@code /idgenerator/v1/...}).
 */
public final class ApiPaths {
    public static final String BASE = "/idgenerator/{version}";
    public static final int VERSION_SEGMENT_INDEX = 1;
    public static final String V1 = "1";

    private ApiPaths() {
    }
}
