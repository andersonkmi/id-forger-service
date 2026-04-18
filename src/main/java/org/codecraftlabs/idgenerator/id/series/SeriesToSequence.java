package org.codecraftlabs.idgenerator.id.series;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum SeriesToSequence {
    DEFAULT("STD", "default", "default_sequence"),
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

    @Nonnull
    public static Optional<SeriesToSequence> findByName(@Nonnull String name) {
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.seriesName, name))
                .findFirst();
    }

    @Nonnull
    public String getPrefix() {
        return prefix;
    }

    @Nonnull
    public String getSeriesName() {
        return seriesName;
    }

    @Nonnull
    public String getSequenceName() {
        return sequenceName;
    }
}
