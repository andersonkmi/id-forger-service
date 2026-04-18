package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.series.SeriesToSequence;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.util.Optional;

import static java.lang.String.format;

/**
 * {@link IdFormatProcessor} that prepends the series prefix and zero-pads the ID to
 * 15 digits, producing identifiers such as {@code STD000000000012345}.
 * Registered as the {@code prefixed} format processor.
 */
@Service("prefixed")
class PrefixedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    PrefixedIdGeneratorProcessor(@Nonnull IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        Optional<SeriesToSequence> seriesToSequence = SeriesToSequence.findByName(seriesName);
        if (seriesToSequence.isEmpty()) {
            throw new InvalidSeriesException("Series not mapped yet");
        }
        long id = idGenerator.generateLongId(seriesName);
        String prefix = seriesToSequence.get().getPrefix();
        return format("%s%015d", prefix, id);
    }
}
