package com.aionn.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "chat.realtime")
public record ChatRealtimeProperties(
        @DefaultValue("stomp") String provider) {
}
