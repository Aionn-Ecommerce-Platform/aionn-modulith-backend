package com.aionn.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class OperationalRuntimeConfigurationTest {

    @Test
    void applicationConfigurationSupportsSafeContainerLifecycle() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));

        assertThat(value(sources, "server.shutdown")).isEqualTo("graceful");
        assertThat(value(sources, "spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("${SHUTDOWN_TIMEOUT:30s}");
        assertThat(value(sources, "management.endpoint.health.probes.enabled")).isEqualTo(true);
        assertThat(value(sources, "management.endpoint.health.validate-group-membership")).isEqualTo(false);
        assertThat(value(sources, "management.endpoint.health.probes.add-additional-paths")).isEqualTo(true);
        assertThat(value(sources, "management.endpoint.health.group.liveness.include"))
                .isEqualTo("livenessState");
        assertThat(value(sources, "management.endpoint.health.group.readiness.include"))
                .isEqualTo("readinessState,db,redis");
    }

    private static Object value(List<PropertySource<?>> sources, String key) {
        return sources.stream()
                .map(source -> source.getProperty(key))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
