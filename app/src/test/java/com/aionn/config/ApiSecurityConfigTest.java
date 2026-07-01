package com.aionn.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class ApiSecurityConfigTest {

    private static final String CORS_ALLOWED_ORIGINS = "SECURITY_CORS_ALLOWED_ORIGINS";

    private final ApiSecurityConfig apiSecurityConfig = new ApiSecurityConfig();

    @AfterEach
    void clearCorsProperty() {
        System.clearProperty(CORS_ALLOWED_ORIGINS);
    }

    @Test
    void corsConfigurationFallsBackToLocalhostWhenNoOriginsConfigured() {
        System.clearProperty(CORS_ALLOWED_ORIGINS);

        CorsConfigurationSource source = apiSecurityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertEquals(List.of("http://localhost:3000"), configuration.getAllowedOrigins());
    }

    @Test
    void corsConfigurationFiltersBlankOriginsAndExposesTracingHeaders() {
        System.setProperty(
                CORS_ALLOWED_ORIGINS,
                " https://frontend.example , , http://localhost:3000  ");

        CorsConfigurationSource source = apiSecurityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertEquals(
                List.of("https://frontend.example", "http://localhost:3000"),
                configuration.getAllowedOrigins());
        assertEquals(List.of("X-Request-Id", "Idempotent-Replay"), configuration.getExposedHeaders());
        assertEquals(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                configuration.getAllowedMethods());
    }
}
