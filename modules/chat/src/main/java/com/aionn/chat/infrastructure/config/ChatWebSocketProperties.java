package com.aionn.chat.infrastructure.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "chat.websocket")
public record ChatWebSocketProperties(
        @DefaultValue("http://localhost:3000") List<String> allowedOrigins,
        @Min(0) @DefaultValue("10000") long clientHeartbeatMs,
        @Min(0) @DefaultValue("10000") long serverHeartbeatMs) {
}
