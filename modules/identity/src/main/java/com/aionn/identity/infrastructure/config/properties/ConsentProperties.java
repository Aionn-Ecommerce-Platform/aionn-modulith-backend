package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.consent")
public record ConsentProperties(
        @DefaultValue("terms-v1") String termsVersion,
        @DefaultValue("privacy-v1") String privacyVersion,
        @DefaultValue("marketing-v1") String marketingVersion) {
}
