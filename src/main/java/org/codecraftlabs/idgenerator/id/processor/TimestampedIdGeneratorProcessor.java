package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.time.Instant;

import static java.lang.String.format;

@Service("timestamped")
class TimestampedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdManager idManager;

    @Autowired
    TimestampedIdGeneratorProcessor(@Nonnull IdManager idManager) {
        this.idManager = idManager;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        long id = idManager.generateLongId(seriesName);
        return format("%015d-%d", id, Instant.now().toEpochMilli());
    }
}
