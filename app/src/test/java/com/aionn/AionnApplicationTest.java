package com.aionn;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class AionnApplicationTest {

    @Test
    void loadDotenvDefaultsReturnsAStableMapEvenWhenEnvFileIsMissing() {
        Map<String, Object> defaults = AionnApplication.loadDotenvDefaults();

        assertNotNull(defaults);
    }

    @Test
    void createApplicationBuildsSpringApplicationWithDefaults() {
        SpringApplication application = AionnApplication.createApplication();

        assertNotNull(application);
    }
}
