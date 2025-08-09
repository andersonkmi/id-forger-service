package org.codecraftlabs.idgenerator.id.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.time.Instant;

import static java.lang.String.format;

@Service("timestamped")
class TimestampedIdGeneratorProcessor implements IdFormatProcessor {
    private final SimpleIdGenerator simpleIdGenerator;

    @Autowired
    TimestampedIdGeneratorProcessor(@Nonnull SimpleIdGenerator simpleIdGenerator) {
        this.simpleIdGenerator = simpleIdGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        long id = simpleIdGenerator.generateLongId(seriesName);
        return format("%015d-%d", id, Instant.now().toEpochMilli());
    }
}
