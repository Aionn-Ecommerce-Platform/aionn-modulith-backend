package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "identity.auth.session")
public record AuthSessionProperties(
        Long expiresDays,
        Long retentionDays) {

    @ConstructorBinding
    public AuthSessionProperties {
        if (expiresDays == null) {
            expiresDays = 30L;
        }
        if (retentionDays == null) {
            retentionDays = 90L;
        }
    }

    public AuthSessionProperties(Long expiresDays) {
        this(expiresDays, null);
    }
}

