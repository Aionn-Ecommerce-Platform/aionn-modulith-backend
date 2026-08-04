package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.AuthPolicy;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.identity.infrastructure.config.properties.AuthSessionProperties;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringAuthPolicy implements AuthPolicy {

    private final AuthSessionProperties properties;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;

    @Override
    public long getSessionExpiresDays() {
        return properties.expiresDays();
    }

    @Override
    public int getAccessTokenExpiryMinutes() {
        return jwtProperties.accessTokenExpiryMinutes();
    }

    @Override
    public int getMaxFailedLoginAttempts() {
        return authProperties.maxFailedLoginAttempts();
    }

    @Override
    public int getLockoutMinutes() {
        return authProperties.lockoutMinutes();
    }

    @Override
    public int getPasswordResetTokenTtlMinutes() {
        return authProperties.passwordResetTokenTtlMinutes();
    }

    @Override
    public int getLoginIpMaxAttempts() {
        return authProperties.loginIpMaxAttempts();
    }

    @Override
    public int getLoginIdentityMaxAttempts() {
        return authProperties.loginIdentityMaxAttempts();
    }

    @Override
    public int getLoginRateLimitWindowSeconds() {
        return authProperties.loginRateLimitWindowSeconds();
    }

    @Override
    public int getPasswordResetIpMaxAttempts() {
        return authProperties.passwordResetIpMaxAttempts();
    }

    @Override
    public int getPasswordResetIdentityMaxAttempts() {
        return authProperties.passwordResetIdentityMaxAttempts();
    }

    @Override
    public int getPasswordResetRateLimitWindowSeconds() {
        return authProperties.passwordResetRateLimitWindowSeconds();
    }
}
