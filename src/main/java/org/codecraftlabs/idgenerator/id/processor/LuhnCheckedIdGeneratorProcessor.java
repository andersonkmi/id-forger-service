package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.codecraftlabs.idgenerator.id.util.LuhnDigitNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("luhn")
class LuhnCheckedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdManager idManager;
    private final LuhnDigitNumberGenerator luhnDigitNumberGenerator;

    @Autowired
    LuhnCheckedIdGeneratorProcessor(@Nonnull IdManager idManager, @Nonnull LuhnDigitNumberGenerator luhnDigitNumberGenerator) {
        this.idManager = idManager;
        this.luhnDigitNumberGenerator = luhnDigitNumberGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String id = idManager.generateId(seriesName);
        return luhnDigitNumberGenerator.generatorLuhnCheckValidNumber(id);
    }
}
