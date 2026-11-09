package org.codecraftlabs.idgenerator.id.series;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(SeriesToSequenceMapper.class);
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
    public Optional<SeriesInfo> getSeriesDetails(@Nonnull String seriesName) {
        var result = Optional.ofNullable(seriesDetails.get(seriesName));
        if (result.isPresent()) {
            LOGGER.atInfo().log(String.format("%s details found in cache", seriesName));
            return result;
        }

        var details = uniqueIdMapperRepository.getSeriesDetails(seriesName);
        if (details == null) {
            return Optional.empty();
        }

        SeriesInfo seriesInfo = new SeriesInfo(details.sequence(), details.prefix());
        LOGGER.atInfo().log(String.format("%s details loaded from the database", seriesName));
        addSeriesDetails(seriesName, seriesInfo);
        return Optional.of(seriesInfo);
    }

    /**
     * Stores the details of a series in the cache.
     *
     * @param seriesName the series name
     * @param seriesInfo the sequence name and prefix of the series
     */
    private void addSeriesDetails(@Nonnull String seriesName,
                                  @Nonnull SeriesInfo seriesInfo) {
        LOGGER.atInfo().log(String.format("%s details added into the cache", seriesName));
        seriesDetails.put(seriesName, seriesInfo);
    }
}
