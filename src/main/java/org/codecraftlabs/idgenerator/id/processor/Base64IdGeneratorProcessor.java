package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Base64;

@Service("base64")
class Base64IdGeneratorProcessor implements IdFormatProcessor {
    private final IdManager idManager;

    @Autowired
    Base64IdGeneratorProcessor(@Nonnull IdManager idManager) {
        this.idManager = idManager;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String value = idManager.generateId(seriesName);
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
