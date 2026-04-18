package org.codecraftlabs.idgenerator.id.util;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;

@Component
@Scope("prototype")
public class LuhnDigitNumberGenerator {
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
