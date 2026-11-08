package org.codecraftlabs.idgenerator.id.series;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves series names to their backing sequence and identifier prefix.
 *
 * <p>Lookups are served from an in-memory cache. On a cache miss the details are loaded
 * from the {@code series_details} table through {@link UniqueIdMapperRepository} and
 * cached for subsequent calls. The cache is thread-safe.</p>
 */
@Component
public class SeriesToSequenceMapper {
    private final Map<String, SeriesInfo> seriesDetails = new ConcurrentHashMap<>();
    private final UniqueIdMapperRepository uniqueIdMapperRepository;

    /**
     * Constructs a new {@code SeriesToSequenceMapper}.
     *
     * @param uniqueIdMapperRepository the repository used to load series details on a cache miss
     */
    @Autowired
    public SeriesToSequenceMapper(@Nonnull UniqueIdMapperRepository uniqueIdMapperRepository) {
        this.uniqueIdMapperRepository = uniqueIdMapperRepository;
    }

    /**
     * Returns the sequence name and prefix associated with the given series, loading them
     * from the database and caching them if they are not cached yet.
     *
     * @param seriesName the series name
     * @return the {@link SeriesInfo} for the series
     * @throws org.codecraftlabs.idgenerator.id.repository.DatabaseException if loading the details fails
     */
    @Nonnull
    public SeriesInfo getSeriesDetails(@Nonnull String seriesName) {
        var result = Optional.ofNullable(seriesDetails.get(seriesName));
        if (result.isPresent()) {
            return result.get();
        }

        var details = uniqueIdMapperRepository.getSeriesDetails(seriesName);
        SeriesInfo seriesInfo = new SeriesInfo(details.sequence(), details.prefix());
        addSeriesDetails(seriesName, seriesInfo);
        return seriesInfo;
    }

    /**
     * Stores the details of a series in the cache.
     *
     * @param seriesName the series name
     * @param seriesInfo the sequence name and prefix of the series
     */
    private void addSeriesDetails(@Nonnull String seriesName,
                                  @Nonnull SeriesInfo seriesInfo) {
        seriesDetails.put(seriesName, seriesInfo);
    }
}
