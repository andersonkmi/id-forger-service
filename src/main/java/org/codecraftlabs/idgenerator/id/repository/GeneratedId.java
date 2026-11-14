package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;

/**
 * A freshly generated sequence value together with the prefix configured for its series,
 * both read in the same database round trip.
 *
 * @param value  the value obtained by advancing the series' backing sequence
 * @param prefix the prefix applied to identifiers generated for the series
 */
public record GeneratedId(long value, @Nonnull String prefix) {
}
