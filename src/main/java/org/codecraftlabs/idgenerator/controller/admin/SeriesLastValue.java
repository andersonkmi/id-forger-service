package org.codecraftlabs.idgenerator.controller.admin;

import jakarta.validation.constraints.NotNull;

/**
 * Request body used when updating the last value of a sequence via the PATCH endpoint.
 *
 * <p>The field is a boxed {@link Long} so that a missing or {@code null} value is rejected
 * by bean validation instead of silently defaulting to {@code 0}.</p>
 *
 * @param newLastValue the desired last value; required
 */
public record SeriesLastValue(@NotNull(message = "newLastValue is required") Long newLastValue) {
}
