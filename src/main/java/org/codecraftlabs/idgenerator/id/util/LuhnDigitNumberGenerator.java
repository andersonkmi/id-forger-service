package org.codecraftlabs.idgenerator.id.util;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Computes the Luhn check digit for a partial number and appends it to produce
 * a number that passes Luhn validation.
 */
@Component
public class LuhnDigitNumberGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuhnDigitNumberGenerator.class);

    /**
     * Calculates the Luhn check digit for {@code partialNumber} and appends it.
     *
     * @param partialNumber the number without its check digit
     * @return the complete number with the Luhn check digit appended
     */
    @Nonnull
    public String generatorLuhnCheckValidNumber(@Nonnull String partialNumber) {
        LOGGER.atInfo().log(String.format("Generating Luhn check digit for %s", partialNumber));
        int luhnDigit = calculateCheckDigit(partialNumber);
        LOGGER.atInfo().log(String.format("Generated Luhn check digit %d", luhnDigit));
        return String.format("%s%d", partialNumber, luhnDigit);
    }

    private int calculateCheckDigit(@Nonnull String partialNumber) {
        String numberWithDummy = partialNumber + "0";
        int sum = 0;
        boolean alternate = false;

        for (int i = numberWithDummy.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(numberWithDummy.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit / 10 + digit % 10;
                }
            }

            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }
}
