package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.series.SeriesSequenceMapper;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UniqueIdMapperRepository {
    private final UniqueIdMapper uniqueIdMapper;
    private final SeriesSequenceMapper seriesSequenceMapper;

    public UniqueIdMapperRepository(@Nonnull UniqueIdMapper uniqueIdMapper,
                                    @Nonnull SeriesSequenceMapper seriesSequenceMapper) {
        this.uniqueIdMapper = uniqueIdMapper;
        this.seriesSequenceMapper = seriesSequenceMapper;
    }

    public long getNextId(@Nonnull String seriesName) {
        try {
            String sequenceName = getSequenceName(seriesName);
            return uniqueIdMapper.getNextId(sequenceName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the next sequence value", exception);
        }
    }

    public long getCurrentId(@Nonnull String seriesName) {
        try {
            String sequenceName = getSequenceName(seriesName);
            return uniqueIdMapper.getCurrentSequenceValue(sequenceName);
        } catch (DataAccessException exception) {
            throw new DatabaseException("Failed to get the current sequence value", exception);
        }
    }

    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name) {
        return uniqueIdMapper.getSequenceDetails(schema, getSequenceName(name));
    }

    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        try {
            String sequenceName = getSequenceName(name);
            this.uniqueIdMapper.updateSequenceLastValue(sequenceName, lastValue);
        } catch (DataAccessException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
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
