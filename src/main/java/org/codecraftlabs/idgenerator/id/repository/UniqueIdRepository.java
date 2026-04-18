package org.codecraftlabs.idgenerator.id.repository;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.series.SeriesSequenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import java.util.Optional;

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

    public long getNextId(@Nonnull String seriesName) {
        String sequenceName = getSequenceName(seriesName);
        return jdbcTemplateDataRepository.getNextSequenceValue(sequenceName);
    }

    public long getCurrentId(@Nonnull String seriesName) {
        String sequenceName = getSequenceName(seriesName);
        return jdbcTemplateDataRepository.getCurrentSequenceValue(sequenceName);
    }

    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name) {
        return jdbcTemplateDataRepository.getSequenceDetails(schema, getSequenceName(name));
    }

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
