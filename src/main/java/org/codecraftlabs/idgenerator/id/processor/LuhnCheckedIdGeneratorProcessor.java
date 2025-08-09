package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.util.LuhnValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service("luhn")
class LuhnCheckedIdGeneratorProcessor implements IdGenerationProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LuhnCheckedIdGeneratorProcessor.class);

    private final SimpleIdGenerator simpleIdGenerator;
    private final LuhnValidator luhnValidator;

    @Autowired
    LuhnCheckedIdGeneratorProcessor(@Nonnull SimpleIdGenerator simpleIdGenerator, @Nonnull LuhnValidator luhnValidator) {
        this.simpleIdGenerator = simpleIdGenerator;
        this.luhnValidator = luhnValidator;
    }

    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String originalValue = simpleIdGenerator.generateId(seriesName);
        int numberOfCalls = 1;
        while(luhnValidator.isInvalid(originalValue)) {
            originalValue = simpleIdGenerator.generateId(seriesName);
            numberOfCalls++;
        }
        logger.info("Number of calls until finding a valid luhn checked number: '{}'", numberOfCalls);
        return originalValue;
    }
}
