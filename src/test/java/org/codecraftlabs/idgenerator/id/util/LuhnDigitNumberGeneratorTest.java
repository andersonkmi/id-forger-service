package org.codecraftlabs.idgenerator.id.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
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
    public void should_not_log_at_info_level_on_the_hot_path() {
        Logger logger = (Logger) LoggerFactory.getLogger(LuhnDigitNumberGenerator.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            luhnDigitNumberGenerator.generatorLuhnCheckValidNumber("12345");

            assertThat(appender.list)
                    .noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.INFO));
        } finally {
            logger.detachAppender(appender);
        }
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
