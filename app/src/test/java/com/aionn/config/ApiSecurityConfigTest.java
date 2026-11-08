package com.aionn.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class ApiSecurityConfigTest {

    /**
     * An ObjectProvider with no bean — enough for these CORS-only tests, which never
     * trigger {@code ifAvailable}'s filter wiring.
     */
    @SuppressWarnings("unchecked")
    private static ObjectProvider<com.aionn.ucp.adapter.rest.filter.UcpAgentAuthenticationFilter> emptyProvider() {
        ObjectProvider<com.aionn.ucp.adapter.rest.filter.UcpAgentAuthenticationFilter> provider =
                org.mockito.Mockito.mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        return provider;
    }


    @Test
    void corsConfigurationFallsBackToLocalhostWhenNoOriginsConfigured() {
        ApiSecurityConfig apiSecurityConfig = new ApiSecurityConfig("", null, emptyProvider());

        CorsConfigurationSource source = apiSecurityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertEquals(List.of("http://localhost:3000"), configuration.getAllowedOrigins());
    }

    @Test
    void corsConfigurationFiltersBlankOriginsAndExposesTracingHeaders() {
        ApiSecurityConfig apiSecurityConfig =
                new ApiSecurityConfig(" https://frontend.example , , http://localhost:3000  ", null, emptyProvider());

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
