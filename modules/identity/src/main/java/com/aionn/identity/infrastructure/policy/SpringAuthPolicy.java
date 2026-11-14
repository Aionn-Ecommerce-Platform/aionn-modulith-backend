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
    public long sessionExpiresDays() {
        return properties.expiresDays();
    }

    @Override
    public int accessTokenExpiryMinutes() {
        return jwtProperties.accessTokenExpiryMinutes();
    }

    @Override
    public int maxFailedLoginAttempts() {
        return authProperties.maxFailedLoginAttempts();
    }

    @Override
    public int lockoutMinutes() {
        return authProperties.lockoutMinutes();
    }

    @Override
    public int passwordResetTokenTtlMinutes() {
        return authProperties.passwordResetTokenTtlMinutes();
    }

    @Override
    public int loginIpMaxAttempts() {
        return authProperties.loginIpMaxAttempts();
    }

    @Override
    public int loginIdentityMaxAttempts() {
        return authProperties.loginIdentityMaxAttempts();
    }

    @Override
    public int loginRateLimitWindowSeconds() {
        return authProperties.loginRateLimitWindowSeconds();
    }

    @Override
    public int passwordResetIpMaxAttempts() {
        return authProperties.passwordResetIpMaxAttempts();
    }

    @Override
    public int passwordResetIdentityMaxAttempts() {
        return authProperties.passwordResetIdentityMaxAttempts();
    }

    @Override
    public int passwordResetRateLimitWindowSeconds() {
        return authProperties.passwordResetRateLimitWindowSeconds();
    }

}
