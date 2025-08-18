package org.codecraftlabs.idgenerator.id.service.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("plain")
class PlainIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    @Autowired
    PlainIdGeneratorProcessor(@Nonnull IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        return idGenerator.generateId(seriesName);
    }
}
