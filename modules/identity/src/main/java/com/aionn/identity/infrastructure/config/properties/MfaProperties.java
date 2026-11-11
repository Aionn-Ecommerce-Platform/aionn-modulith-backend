package com.aionn.identity.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "identity.mfa")
public record MfaProperties(
        @DefaultValue("Aionn") String issuer,
        @DefaultValue(MfaProperties.DEFAULT_ENCRYPTION_KEY) String encryptionKey,
        @Min(1) @Max(20) @DefaultValue("8") int backupCodeCount,
        @Min(1) @Max(9) @DefaultValue("8") int backupCodeLength) {

    @ConstructorBinding
    public MfaProperties {
    }

    public MfaProperties(String issuer, String encryptionKey, int backupCodeCount) {
        this(issuer, encryptionKey, backupCodeCount, 8);
    }

    public static final String DEFAULT_ENCRYPTION_KEY = "dev-only-mfa-encryption-key-override-in-prod";
}
