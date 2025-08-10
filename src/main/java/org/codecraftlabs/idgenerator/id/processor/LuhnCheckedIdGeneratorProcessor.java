package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.codecraftlabs.idgenerator.id.util.LuhnValidNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("luhn")
class LuhnCheckedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdManager idManager;
    private final LuhnValidNumberGenerator luhnValidNumberGenerator;

    @Autowired
    LuhnCheckedIdGeneratorProcessor(@Nonnull IdManager idManager, @Nonnull LuhnValidNumberGenerator luhnValidNumberGenerator) {
        this.idManager = idManager;
        this.luhnValidNumberGenerator = luhnValidNumberGenerator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String id = idManager.generateId(seriesName);
        return luhnValidNumberGenerator.generatorLuhnCheckValidNumber(id);
    }
}
