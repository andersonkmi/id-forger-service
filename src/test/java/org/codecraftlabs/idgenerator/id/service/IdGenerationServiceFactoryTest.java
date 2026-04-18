package org.codecraftlabs.idgenerator.id.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class IdGenerationServiceFactoryTest {
    @Autowired
    private IdGenerationServiceFactory idGenerationServiceFactory;

    @ParameterizedTest
    @ValueSource(strings = {"plain", "base64", "prefixed", "luhn", "sha256", "timestamped"})
    public void all_registered_processors_should_be_present(String format) {
        assertThat(idGenerationServiceFactory.getProcessor(format)).isPresent();
    }

    @Test
    public void unknown_format_should_return_empty() {
        assertThat(idGenerationServiceFactory.getProcessor("unknown")).isEmpty();
    }
}
