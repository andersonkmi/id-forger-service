package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.util.LuhnDigitNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

/**
 * {@link IdFormatProcessor} that appends a Luhn check digit to the generated numeric ID.
 * The resulting number passes the standard Luhn validation algorithm.
 * Registered as the {@code luhn} format processor.
 */
@Service("luhn")
class LuhnCheckedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;
    private final LuhnDigitNumberGenerator luhnDigitNumberGenerator;

    @Autowired
    LuhnCheckedIdGeneratorProcessor(@Nonnull IdGenerator idGenerator, @Nonnull LuhnDigitNumberGenerator luhnDigitNumberGenerator) {
        this.idGenerator = idGenerator;
        this.luhnDigitNumberGenerator = luhnDigitNumberGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String id = idGenerator.generateId(seriesName);
        return luhnDigitNumberGenerator.generatorLuhnCheckValidNumber(id);
    }
}
