package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.presence")
public record ChatPresenceProperties(
        @DefaultValue("redis") String provider) {
}
