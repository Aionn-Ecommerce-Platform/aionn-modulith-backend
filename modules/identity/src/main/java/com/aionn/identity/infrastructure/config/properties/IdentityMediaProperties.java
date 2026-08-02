package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.media")
public record IdentityMediaProperties(
        @DefaultValue("cloudinary") String provider) {
}
