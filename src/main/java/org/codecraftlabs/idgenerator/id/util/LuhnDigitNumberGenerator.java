package org.codecraftlabs.idgenerator.id.util;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;

/**
 * Computes the Luhn check digit for a partial number and appends it to produce
 * a number that passes Luhn validation.
 */
@Component
@Scope("prototype")
public class LuhnDigitNumberGenerator {
    /**
     * Calculates the Luhn check digit for {@code partialNumber} and appends it.
     *
     * @param partialNumber the number without its check digit
     * @return the complete number with the Luhn check digit appended
     */
    @Nonnull
    public String generatorLuhnCheckValidNumber(@Nonnull String partialNumber) {
        int luhnDigit = calculateCheckDigit(partialNumber);
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
