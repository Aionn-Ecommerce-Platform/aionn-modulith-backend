package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.mfa")
public record MfaProperties(
        @DefaultValue("Aionn") String issuer,
        @DefaultValue(MfaProperties.DEFAULT_ENCRYPTION_KEY) String encryptionKey,
        @DefaultValue("8") int backupCodeCount) {

    public static final String DEFAULT_ENCRYPTION_KEY = "dev-only-mfa-encryption-key-override-in-prod";
}
