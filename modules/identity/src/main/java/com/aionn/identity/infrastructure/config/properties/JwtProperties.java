package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.jwt")
public record JwtProperties(
                @DefaultValue("aionn-identity") String issuer,
                String secret,
                @DefaultValue("15") int accessTokenExpiryMinutes) {
}
