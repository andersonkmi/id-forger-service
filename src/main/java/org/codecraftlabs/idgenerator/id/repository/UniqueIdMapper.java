package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import org.apache.ibatis.annotations.Mapper;
import org.codecraftlabs.idgenerator.id.Sequence;

@Mapper
public interface UniqueIdMapper {
    long getNextId(@Nonnull String seriesName);
    long getCurrentSequenceValue(@Nonnull String seriesName);
    Sequence getSequenceDetails(@Nonnull String schema, @Nonnull String name);
    void updateSequenceLastValue(@Nonnull String name, long lastValue);
}
