package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.series.SeriesToSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.lang.String.format;

@Service("prefixed")
class PrefixedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    @Autowired
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
