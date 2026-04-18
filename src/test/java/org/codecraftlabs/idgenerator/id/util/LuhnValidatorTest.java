package org.codecraftlabs.idgenerator.id.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class LuhnValidatorTest {
    private LuhnValidator luhnValidator;

    @BeforeEach
    public void setup() {
        this.luhnValidator = new LuhnValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"18", "00", "1206", "1008", "79927398713"})
    public void valid_luhn_numbers_should_return_true(String number) {
        assertThat(luhnValidator.isValid(number)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "100", "1234", "79927398714"})
    public void invalid_luhn_numbers_should_return_false(String number) {
        assertThat(luhnValidator.isValid(number)).isFalse();
    }
}
