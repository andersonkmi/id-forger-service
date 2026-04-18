package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.series.SeriesSequenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import java.util.Optional;

/**
 * Repository facade that translates series names to database sequence names and
 * delegates all JDBC operations to {@link JdbcTemplateDataRepository}.
 */
@Component
public class UniqueIdRepository {
    private final JdbcTemplateDataRepository jdbcTemplateDataRepository;
    private final SeriesSequenceMapper seriesSequenceMapper;

    @Autowired
    public UniqueIdRepository(@Nonnull JdbcTemplateDataRepository jdbcTemplateDataRepository,
                              @Nonnull SeriesSequenceMapper seriesSequenceMapper) {
        this.jdbcTemplateDataRepository = jdbcTemplateDataRepository;
        this.seriesSequenceMapper = seriesSequenceMapper;
    }

    /**
     * Advances the sequence for the given series and returns the next value.
     *
     * @param seriesName the series name (e.g. {@code default})
     * @return the next sequence value
     * @throws SequenceNotFoundException if the series name is not mapped to a sequence
     */
    public long getNextId(@Nonnull String seriesName) {
        String sequenceName = getSequenceName(seriesName);
        return jdbcTemplateDataRepository.getNextSequenceValue(sequenceName);
    }

    /**
     * Returns the current last value of the sequence without advancing it.
     *
     * @param seriesName the series name
     * @return the current sequence value
     * @throws SequenceNotFoundException if the series name is not mapped to a sequence
     */
    public long getCurrentId(@Nonnull String seriesName) {
        String sequenceName = getSequenceName(seriesName);
        return jdbcTemplateDataRepository.getCurrentSequenceValue(sequenceName);
    }

    /**
     * Retrieves full metadata for the sequence identified by the given schema and series name.
     *
     * @param schema database schema (e.g. {@code public})
     * @param name   series name used to look up the sequence name
     * @return the {@link Sequence} metadata
     */
    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name) {
        return jdbcTemplateDataRepository.getSequenceDetails(schema, getSequenceName(name));
    }

    /**
     * Updates the last value of the sequence backing the given series.
     *
     * @param name      series name used to look up the sequence name
     * @param lastValue the new last value to set
     */
    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        this.jdbcTemplateDataRepository.updateSequenceLastValue(getSequenceName(name), lastValue);
    }

    @Nonnull
    private String getSequenceName(@Nonnull String type) {
        Optional<String> sequenceName = seriesSequenceMapper.getSequenceBySeriesName(type);
        if (sequenceName.isEmpty()) {
            throw new SequenceNotFoundException("Invalid sequence name");
        }
        return sequenceName.get();
    }
}
