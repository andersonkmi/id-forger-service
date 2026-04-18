package org.codecraftlabs.idgenerator.id.service.processor;

import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.time.Instant;

import static java.lang.String.format;

/**
 * {@link IdFormatProcessor} that appends the current epoch millisecond timestamp to the ID,
 * producing identifiers in the format {@code <15-digit-id>-<epoch-millis>}.
 * Registered as the {@code timestamped} format processor.
 */
@Service("timestamped")
class TimestampedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

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
