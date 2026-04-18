package org.codecraftlabs.idgenerator.controller;

/**
 * Response wrapper that carries a single generated ID value.
 *
 * @param id the generated identifier string
 */
public record IdResponse(String id) {
}
