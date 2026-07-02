package com.aionn.identity.infrastructure.config.properties;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Builder
@ConfigurationProperties(prefix = "identity.account")
public record AccountManagementProperties(
        @DefaultValue Otp otp,
        @DefaultValue Deletion deletion) {

    @Builder
    public record Otp(
            @DefaultValue("300") int expirySeconds,
            @DefaultValue("5") int maxAttempts) {
    }

    @Builder
    public record Deletion(
            @DefaultValue("30") int graceDays) {
    }
}
