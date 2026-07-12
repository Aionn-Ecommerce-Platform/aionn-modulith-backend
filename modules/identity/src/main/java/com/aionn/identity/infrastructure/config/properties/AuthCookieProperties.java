package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.auth.cookie")
public record AuthCookieProperties(
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite,
        @DefaultValue("/api/v1/auth") String refreshPath) {

    public AuthCookieProperties {
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Strict";
        }
        if (refreshPath == null || refreshPath.isBlank()) {
            refreshPath = "/api/v1/auth";
        }
        if (!refreshPath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "identity.auth.cookie.refresh-path must start with '/', got: " + refreshPath);
        }
    }
}
