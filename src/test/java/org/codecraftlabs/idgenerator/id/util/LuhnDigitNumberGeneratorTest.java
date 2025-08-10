package org.codecraftlabs.idgenerator.id.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LuhnDigitNumberGeneratorTest {
    private LuhnDigitNumberGenerator luhnDigitNumberGenerator;

    @BeforeEach
    public void setup() {
        this.luhnDigitNumberGenerator = new LuhnDigitNumberGenerator();
    }

    @Test
    public void generate_luhn_digit_number() {
        String result = this.luhnDigitNumberGenerator.generatorLuhnCheckValidNumber("120");
        Assertions.assertThat(result).isEqualTo("1206");
    }
}
