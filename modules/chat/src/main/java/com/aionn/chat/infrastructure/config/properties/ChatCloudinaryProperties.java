package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.media.cloudinary")
public record ChatCloudinaryProperties(
        @DefaultValue("aionn/chat/images") String chatImageFolder) {
}
