package com.aionn.identity.infrastructure.config.properties;

import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Builder
@ConfigurationProperties(prefix = "identity.auth")
public record AuthProperties(
        @DefaultValue("X-Client-Type") String clientTypeHeader,
        @DefaultValue("mobile") String mobileClientValue,
        @DefaultValue("5") int maxFailedLoginAttempts,
        @DefaultValue("15") int lockoutMinutes,
        @DefaultValue("15") int passwordResetTokenTtlMinutes,
        @DefaultValue("30") int loginIpMaxAttempts,
        @DefaultValue("10") int loginIdentityMaxAttempts,
        @DefaultValue("300") int loginRateLimitWindowSeconds,
        @DefaultValue("10") int passwordResetIpMaxAttempts,
        @DefaultValue("3") int passwordResetIdentityMaxAttempts,
        @DefaultValue("900") int passwordResetRateLimitWindowSeconds) {
}
