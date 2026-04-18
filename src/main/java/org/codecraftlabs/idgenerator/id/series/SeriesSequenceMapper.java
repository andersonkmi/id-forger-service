package org.codecraftlabs.idgenerator.id.series;

import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

/**
 * In-memory registry that maps series names (e.g. {@code default}) to their
 * corresponding database sequence names (e.g. {@code default_sequence}).
 * Populated from {@link SeriesToSequence} at construction time.
 */
@Component
public class SeriesSequenceMapper {
    private final Map<String, String> sequenceMapper = new ConcurrentHashMap<>();

    public SeriesSequenceMapper() {
        Arrays.stream(SeriesToSequence.values())
                .forEach(item -> sequenceMapper.put(item.getSeriesName(),
                        item.getSequenceName()));
    }

    /**
     * Looks up the database sequence name for the given series.
     *
     * @param seriesName the user-facing series name
     * @return the sequence name wrapped in an {@link Optional}, or empty if unknown
     */
    @Nonnull
    public Optional<String> getSequenceBySeriesName(@Nonnull String seriesName) {
        return ofNullable(this.sequenceMapper.get(seriesName));
    }
}
