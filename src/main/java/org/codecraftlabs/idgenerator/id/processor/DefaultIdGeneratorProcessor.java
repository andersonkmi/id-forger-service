package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("default")
class DefaultIdGeneratorProcessor implements IdFormatProcessor {
    private final IdManager idManager;

    @Autowired
    DefaultIdGeneratorProcessor(@Nonnull IdManager idManager) {
        this.idManager = idManager;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        return idManager.generateId(seriesName);
    }
}
