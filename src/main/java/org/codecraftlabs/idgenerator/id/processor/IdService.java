package org.codecraftlabs.idgenerator.id.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Optional;

@Service
public class IdService {
    private final IdGenerationServiceFactory idGenerationServiceFactory;
    private final IdGenerator idGenerator;

    @Autowired
    public IdService(@Nonnull IdGenerationServiceFactory idGenerationServiceFactory,
                     @Nonnull IdGenerator idGenerator) {
        this.idGenerationServiceFactory = idGenerationServiceFactory;
        this.idGenerator = idGenerator;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName,
                             @CheckForNull String format) {
        String type = getIdGeneratorProcessorType(format);
        Optional<IdFormatProcessor> processor = idGenerationServiceFactory.getProcessor(type);
        IdFormatProcessor idProcessor = processor.orElseThrow(() -> new InvalidFormatException("Invalid format requested"));
        return idProcessor.generateId(seriesName);
    }

    @Nonnull
    public String getCurrentValue(@Nonnull String seriesName) {
        return idGenerator.getCurrentValue(seriesName);
    }

    @Nonnull
    private String getIdGeneratorProcessorType(@CheckForNull String format) {
        return format != null && !format.isBlank() ? format : "plain";
    }
}
