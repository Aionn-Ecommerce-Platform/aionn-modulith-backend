package com.aionn.chat.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.presence")
public record ChatPresenceProperties(
        @DefaultValue("redis") String provider,
        @Min(1) @Max(86_400) @DefaultValue("300") long ttlSeconds,
        @Min(1) @DefaultValue("60000") long refreshMs) {

    @jakarta.validation.constraints.AssertTrue(message = "chat.presence.refresh-ms must be less than ttl-seconds")
    public boolean isRefreshIntervalValid() {
        return refreshMs < ttlSeconds * 1000;
    }
}
