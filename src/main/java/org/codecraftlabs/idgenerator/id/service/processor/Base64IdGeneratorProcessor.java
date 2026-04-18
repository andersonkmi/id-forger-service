package org.codecraftlabs.idgenerator.id.service.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.util.Base64;

/**
 * {@link IdFormatProcessor} that Base64-encodes the raw numeric ID string.
 * Registered as the {@code base64} format processor.
 */
@Service("base64")
class Base64IdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    @Autowired
    Base64IdGeneratorProcessor(@Nonnull IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String value = idGenerator.generateId(seriesName);
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
