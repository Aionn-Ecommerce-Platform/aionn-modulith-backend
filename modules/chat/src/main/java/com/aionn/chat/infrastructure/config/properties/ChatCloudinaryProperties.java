package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.media.cloudinary")
public record ChatCloudinaryProperties(
        @DefaultValue("aionn/chat/images") String chatImageFolder,
        @DefaultValue("jpg,jpeg,png,webp,gif,mp4,mov,webm") String allowedFormats,
        @DefaultValue("26214400") long maxFileSizeBytes) {
}
