package org.codecraftlabs.idgenerator;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Stops the application at startup when the database credentials are missing.
 * Spring Boot leaves unresolved placeholders as literal text when binding the
 * datasource, so without this check the service would start and only fail once
 * a connection is opened.
 */
@Component
public class DatabaseCredentialsCheck {
    /**
     * Resolves the datasource credentials, failing if a placeholder has no value.
     *
     * @param environment the Spring environment
     * @throws IllegalArgumentException if a credential placeholder cannot be resolved
     * @throws IllegalStateException if a credential property is not defined
     */
    public DatabaseCredentialsCheck(Environment environment) {
        environment.getRequiredProperty("spring.datasource.username");
        environment.getRequiredProperty("spring.datasource.password");
    }
}
