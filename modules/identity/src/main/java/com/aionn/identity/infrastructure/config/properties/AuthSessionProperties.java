package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.auth.session")
public record AuthSessionProperties(
        Long expiresDays) {

    public AuthSessionProperties {
        if (expiresDays == null) {
            expiresDays = 30L;
        }
    }
}

