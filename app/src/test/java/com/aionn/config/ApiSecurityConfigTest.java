package com.aionn.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aionn.ucp.adapter.rest.filter.UcpAgentAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiSecurityConfigTest {

    private static ObjectProvider<UcpAgentAuthenticationFilter> emptyProvider() {
        return new DefaultListableBeanFactory().getBeanProvider(UcpAgentAuthenticationFilter.class);
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
        ApiSecurityConfig apiSecurityConfig = new ApiSecurityConfig(
                " https://frontend.example , , http://localhost:3000  ", null, emptyProvider());

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

    @Test
    void csrfAppliesOnlyToCookieAuthenticatedRequests() {
        MockHttpServletRequest anonymous = new MockHttpServletRequest();
        assertFalse(ApiSecurityConfig.requiresCsrfProtection(anonymous));

        MockHttpServletRequest refreshCookie = new MockHttpServletRequest();
        refreshCookie.setCookies(new Cookie("refresh_token", "token"));
        assertTrue(ApiSecurityConfig.requiresCsrfProtection(refreshCookie));

        MockHttpServletRequest bearer = new MockHttpServletRequest();
        bearer.setCookies(new Cookie("refresh_token", "token"));
        bearer.addHeader("Authorization", "Bearer access-token");
        assertFalse(ApiSecurityConfig.requiresCsrfProtection(bearer));

        MockHttpServletRequest unrelatedCookie = new MockHttpServletRequest();
        unrelatedCookie.setCookies(new Cookie("XSRF-TOKEN", "value"));
        assertFalse(ApiSecurityConfig.requiresCsrfProtection(unrelatedCookie));
    }

    @Test
    void corsConfigurationAllowsTheCsrfHeader() {
        ApiSecurityConfig apiSecurityConfig = new ApiSecurityConfig("", null, emptyProvider());

        CorsConfiguration configuration = apiSecurityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertTrue(configuration.getAllowedHeaders().contains("X-XSRF-TOKEN"));
    }
}
