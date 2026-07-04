package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the refresh-token cookie issued by the identity module.
 *
 * <p>
 * Bound to the {@code identity.auth.cookie} property namespace.
 * </p>
 */
@ConfigurationProperties(prefix = "identity.auth.cookie")
public record AuthCookieProperties(
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite,
        // Path the refresh cookie is scoped to. Defaults to /api/v1/auth so
        // the cookie is only sent on auth endpoints; override when the API
        // lives behind a gateway with a different prefix.
        @DefaultValue("/api/v1/auth") String refreshPath) {

    public AuthCookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Strict";
        }
        if (refreshPath == null || refreshPath.isBlank()) {
            refreshPath = "/api/v1/auth";
        }
    }
}
