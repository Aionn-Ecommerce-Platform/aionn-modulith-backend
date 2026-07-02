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
        @DefaultValue("Strict") String sameSite) {

    public AuthCookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Strict";
        }
    }
}
