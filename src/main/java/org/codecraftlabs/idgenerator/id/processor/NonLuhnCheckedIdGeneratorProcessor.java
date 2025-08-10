package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.codecraftlabs.idgenerator.id.util.LuhnValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("nonluhn")
class NonLuhnCheckedIdGeneratorProcessor implements IdFormatProcessor {
    private static final Logger logger = LoggerFactory.getLogger(NonLuhnCheckedIdGeneratorProcessor.class);

    private final IdManager idManager;
    private final LuhnValidator luhnValidator;

    @Autowired
    NonLuhnCheckedIdGeneratorProcessor(@Nonnull IdManager idManager, @Nonnull LuhnValidator luhnValidator) {
        this.idManager = idManager;
        this.luhnValidator = luhnValidator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String originalValue = idManager.generateId(seriesName);
        int numberOfCalls = 1;
        while(luhnValidator.isValid(originalValue)) {
            originalValue = idManager.generateId(seriesName);
            numberOfCalls++;
        }
        logger.info("Number of calls until finding a non valid luhn checked number: '{}'", numberOfCalls);
        return originalValue;
    }
}
