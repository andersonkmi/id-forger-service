package org.codecraftlabs.idgenerator.id.series;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the available ID series and their mapping to database sequences.
 * Each constant binds a user-facing series name to a prefix used in formatted IDs
 * and to the underlying PostgreSQL sequence name.
 */
public enum SeriesToSequence {
    /** Standard default series backed by {@code default_sequence}. */
    DEFAULT("STD", "default", "default_sequence"),
    /** Product series backed by {@code product_sequence}. */
    PRODUCT("PROD", "product", "product_sequence");

    private final String prefix;
    private final String seriesName;
    private final String sequenceName;

    SeriesToSequence(@Nonnull String prefix,
                     @Nonnull String seriesName,
                     @Nonnull String sequenceName) {
        this.prefix = prefix;
        this.seriesName = seriesName;
        this.sequenceName = sequenceName;
    }

    /**
     * Finds the enum constant whose series name matches the given value.
     *
     * @param name the user-facing series name to search for
     * @return the matching constant wrapped in an {@link Optional}, or empty if not found
     */
    @Nonnull
    public static Optional<SeriesToSequence> findByName(@Nonnull String name) {
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.seriesName, name))
                .findFirst();
    }

    /** @return the prefix used in prefixed-format IDs (e.g. {@code STD}) */
    @Nonnull
    public String getPrefix() {
        return prefix;
    }

    /** @return the user-facing series name (e.g. {@code default}) */
    @Nonnull
    public String getSeriesName() {
        return seriesName;
    }

    /** @return the database sequence name (e.g. {@code default_sequence}) */
    @Nonnull
    public String getSequenceName() {
        return sequenceName;
    }
}
