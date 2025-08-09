package org.codecraftlabs.idgenerator.id.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("default")
class DefaultIdGeneratorProcessor implements IdFormatProcessor {
    private final SimpleIdGenerator simpleIdGenerator;

    @Autowired
    DefaultIdGeneratorProcessor(@Nonnull SimpleIdGenerator simpleIdGenerator) {
        this.simpleIdGenerator = simpleIdGenerator;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        return simpleIdGenerator.generateId(seriesName);
    }
}
