package org.codecraftlabs.idgenerator.id.series;

import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

@Component
public class SeriesSequenceMapper {
    private final Map<String, String> sequenceMapper = new ConcurrentHashMap<>();

    public SeriesSequenceMapper() {
        Arrays.stream(SeriesToSequence.values())
                .forEach(item -> sequenceMapper.put(item.getSeriesName(),
                        item.getSequenceName()));
    }

    @Nonnull
    public Optional<String> getSequenceBySeriesName(@Nonnull String seriesName) {
        return ofNullable(this.sequenceMapper.get(seriesName));
    }
}
