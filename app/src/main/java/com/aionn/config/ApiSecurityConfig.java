package com.aionn.config;

import com.aionn.sharedkernel.infrastructure.web.security.SecurityIpProperties;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.aionn.identity.infrastructure.security.web.BearerAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Slf4j
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityIpProperties.class)
public class ApiSecurityConfig {

    private final String allowedOrigins;
    private final BearerAuthenticationFilter bearerAuthenticationFilter;

    public ApiSecurityConfig(
            @Value("${SECURITY_CORS_ALLOWED_ORIGINS:}") String allowedOrigins,
            BearerAuthenticationFilter bearerAuthenticationFilter) {
        this.allowedOrigins = allowedOrigins;
        this.bearerAuthenticationFilter = bearerAuthenticationFilter;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentTypeOptions(opt -> {
                        })
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(ref -> ref.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/social-login",
                                "/api/v1/auth/refresh",
                                "/api/v1/registrations/**",
                                "/api/v1/security/password-reset-requests",
                                "/api/v1/security/password-reset",
                                "/api/v1/feedbacks",
                                "/api/v1/kyc/webhooks/sumsub",
                                "/api/v1/payments/webhooks/**",
                                "/api/v1/payments/vnpay/ipn",
                                "/api/v1/shipping/webhooks/carrier")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/geography/**",
                                "/api/v1/catalog/**",
                                "/api/v1/inventory/items",
                                "/api/v1/promotions/flash-sales/active",
                                "/api/v1/promotions/banners",
                                "/api/v1/promotions/campaigns",
                                "/api/v1/promotions/campaigns/*",
                                "/api/v1/promotions/campaigns/*/vouchers",
                                "/api/v1/promotions/shop-vouchers/merchant/*",
                                "/api/v1/payments/vnpay/return")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info")
                        .permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated())
                .addFilterBefore(bearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = resolveAllowedOrigins();
        if (origins.isEmpty()) {
            log.warn("No CORS allowed origins configured; defaulting to http://localhost:3000");
            cfg.setAllowedOrigins(List.of("http://localhost:3000"));
        } else {
            cfg.setAllowedOrigins(origins);
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Client-Type",
                "X-Request-Id",
                "Idempotency-Key",
                "X-Idempotency-Key",
                "X-Forwarded-For",
                "Origin"));
        cfg.setExposedHeaders(List.of("X-Request-Id", "Idempotent-Replay"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
