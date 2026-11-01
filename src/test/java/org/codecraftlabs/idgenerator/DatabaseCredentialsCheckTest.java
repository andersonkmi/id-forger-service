package org.codecraftlabs.idgenerator;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseCredentialsCheckTest {
    @Test
    void acceptsResolvedCredentials() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "${DB_USER}")
                .withProperty("spring.datasource.password", "${DB_PASSWORD}")
                .withProperty("DB_USER", "user")
                .withProperty("DB_PASSWORD", "secret");

        assertThatCode(() -> new DatabaseCredentialsCheck(environment)).doesNotThrowAnyException();
    }

    @Test
    void failsWhenPasswordPlaceholderIsUnresolved() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "user")
                .withProperty("spring.datasource.password", "${DB_PASSWORD}");

        assertThatThrownBy(() -> new DatabaseCredentialsCheck(environment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void failsWhenUsernamePlaceholderIsUnresolved() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "${DB_USER}")
                .withProperty("spring.datasource.password", "secret");

        assertThatThrownBy(() -> new DatabaseCredentialsCheck(environment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DB_USER");
    }
}
