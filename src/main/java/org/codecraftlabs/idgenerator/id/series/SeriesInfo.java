package org.codecraftlabs.idgenerator.id.series;

import jakarta.annotation.Nonnull;

/**
 * Cached resolution of a series, holding its backing database sequence and
 * the prefix applied to the identifiers it generates.
 *
 * @param sequenceName the name of the database sequence backing the series
 * @param prefix       the prefix applied to identifiers generated for the series
 */
public record SeriesInfo(@Nonnull String sequenceName, @Nonnull String prefix) {
}
