package org.codecraftlabs.idgenerator.id.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class LuhnDigitNumberGeneratorTest {
    private LuhnDigitNumberGenerator luhnDigitNumberGenerator;
    private LuhnValidator luhnValidator;

    @BeforeEach
    public void setup() {
        this.luhnDigitNumberGenerator = new LuhnDigitNumberGenerator();
        this.luhnValidator = new LuhnValidator();
    }

    @ParameterizedTest
    @CsvSource({
        "120, 1206",
        "100, 1008",
        "1,   18",
        "0,   00"
    })
    public void should_append_correct_luhn_check_digit(String input, String expected) {
        assertThat(luhnDigitNumberGenerator.generatorLuhnCheckValidNumber(input.trim()))
                .isEqualTo(expected.trim());
    }

    @Test
    public void generated_number_should_always_pass_luhn_validation() {
        String[] inputs = {"1", "100", "120", "9999", "12345", "999999999999999"};
        for (String input : inputs) {
            String result = luhnDigitNumberGenerator.generatorLuhnCheckValidNumber(input);
            assertThat(luhnValidator.isValid(result))
                    .as("Expected '%s' (from input '%s') to pass Luhn validation", result, input)
                    .isTrue();
        }
    }
}
