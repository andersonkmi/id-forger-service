package org.codecraftlabs.idgenerator.id.service.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.time.Instant;

import static java.lang.String.format;

@Service("timestamped")
class TimestampedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    @Autowired
    TimestampedIdGeneratorProcessor(@Nonnull IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        long id = idGenerator.generateLongId(seriesName);
        return format("%015d-%d", id, Instant.now().toEpochMilli());
    }
}
