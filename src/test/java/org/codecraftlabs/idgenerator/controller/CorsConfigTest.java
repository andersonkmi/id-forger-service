package org.codecraftlabs.idgenerator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CorsConfigTest {
    private static final class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }

    @Test
    public void should_register_no_mapping_when_no_origins_configured() {
        var registry = new InspectableCorsRegistry();
        new CorsConfig(new String[0]).addCorsMappings(registry);

        assertThat(registry.configurations()).isEmpty();
    }

    @Test
    public void should_ignore_blank_origins() {
        var registry = new InspectableCorsRegistry();
        new CorsConfig(new String[]{" ", ""}).addCorsMappings(registry);

        assertThat(registry.configurations()).isEmpty();
    }

    @Test
    public void should_map_all_api_paths_for_configured_origins() {
        var registry = new InspectableCorsRegistry();
        new CorsConfig(new String[]{"https://app.example.com", " https://admin.example.com "})
                .addCorsMappings(registry);

        assertThat(registry.configurations()).containsOnlyKeys("/idgenerator/**");
        CorsConfiguration config = registry.configurations().get("/idgenerator/**");
        assertThat(config.getAllowedOrigins())
                .containsExactly("https://app.example.com", "https://admin.example.com");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PATCH", "DELETE");
        assertThat(config.getAllowedHeaders()).contains("Content-Type", "Authorization");
        assertThat(config.getExposedHeaders()).contains("Retry-After");
        assertThat(config.checkOrigin("https://evil.example.com")).isNull();
    }
}
