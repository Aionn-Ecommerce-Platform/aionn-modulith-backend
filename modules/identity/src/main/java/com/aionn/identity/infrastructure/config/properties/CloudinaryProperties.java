package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.media.cloudinary")
public record CloudinaryProperties(
        @DefaultValue("identity/avatars") String avatarFolder,
        @DefaultValue("identity/kyc") String kycFolder) {
}
