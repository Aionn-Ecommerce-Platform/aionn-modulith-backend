package com.aionn.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
        classes = ApiSecurityConfigIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = "SECURITY_CORS_ALLOWED_ORIGINS=https://frontend.example")
class ApiSecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort tokenIssuer;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.aionn.identity.application.port.out.auth.TokenBlacklistPort tokenBlacklist;

    @Test
    void unmatchedRequestRequiresAuthenticationAndReceivesSecurityHeaders() throws Exception {
        mockMvc.perform(get("/test/ping"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void explicitlyPublicRouteDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/products/example"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actuatorEndpointsOutsideAllowListAreDenied() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void validBearerTokenAuthenticatesProtectedRequest() throws Exception {
        when(tokenIssuer.parseClaims("valid-token"))
                .thenReturn(Optional.of(new AccessTokenClaims("user-1", "session-1", null, List.of("USER"))));

        mockMvc.perform(get("/test/ping").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void preflightRequestUsesCorsConfiguration() throws Exception {
        mockMvc.perform(options("/test/ping")
                        .header("Origin", "https://frontend.example")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.example"))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Request-Id, Idempotent-Replay"));
    }

    @Test
    void preflightRequestRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/test/ping")
                        .header("Origin", "https://attacker.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            DataRedisAutoConfiguration.class
    })
    @Import({ApiSecurityConfig.class, com.aionn.identity.infrastructure.security.web.BearerAuthenticationFilter.class})
    static class TestApplication {
    }
}
