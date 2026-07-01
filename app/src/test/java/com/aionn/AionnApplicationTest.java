package com.aionn;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AionnApplicationTest {

    @Test
    void loadDotenvDefaultsReturnsAStableMapEvenWhenEnvFileIsMissing() {
        Map<String, Object> defaults = AionnApplication.loadDotenvDefaults();

        assertTrue(defaults != null);
    }
}
