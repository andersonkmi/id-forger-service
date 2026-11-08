package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;

/**
 * Row of the {@code series_details} table, mapping a series to its backing
 * database sequence and identifier prefix.
 *
 * @param sequence the name of the database sequence backing the series
 * @param prefix   the prefix applied to identifiers generated for the series
 */
public record SeriesDetail(@Nonnull String sequence, @Nonnull String prefix) {
}
