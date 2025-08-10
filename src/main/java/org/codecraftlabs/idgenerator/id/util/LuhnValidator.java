package org.codecraftlabs.idgenerator.id.util;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * The Luhn algorithm is a checksum formula used to validate identification numbers like credit cards, IMEI numbers,
 * and other ID codes. Here's how to verify if a number passes the Luhn check:
 *
 * Start from the rightmost digit (excluding the check digit if you're validating the entire number)
 * Double every second digit moving left
 * If doubling produces a two-digit number, subtract 9 (or add the digits together)
 * Sum all the digits
 * Check if the total is divisible by 10 - if yes, it's valid
 *
 * Example
 * Let's verify credit card number: 4532015112830366
 * Original: 4 5 3 2 0 1 5 1 1 2 8 3 0 3 6 6
 * Double 2nd: 4 10 3 4 0 2 5 2 1 4 8 6 0 6 6 6
 * Subtract 9: 4 1 3 4 0 2 5 2 1 4 8 6 0 6 6 6
 * Sum: 4+1+3+4+0+2+5+2+1+4+8+6+0+6+6+6 = 58
 * Since 58 is not divisible by 10, this number would fail the Luhn check.
 */
@Component
@Scope("prototype")
public class LuhnValidator {
    public boolean isValid(@Nonnull String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
}
