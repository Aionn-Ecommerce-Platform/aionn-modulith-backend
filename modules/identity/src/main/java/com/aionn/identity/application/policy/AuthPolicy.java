package com.aionn.identity.application.policy;

public interface AuthPolicy {

    long sessionExpiresDays();

    int accessTokenExpiryMinutes();

    int maxFailedLoginAttempts();

    int lockoutMinutes();

    int passwordResetTokenTtlMinutes();

    int loginIpMaxAttempts();

    int loginIdentityMaxAttempts();

    int loginRateLimitWindowSeconds();

    int passwordResetIpMaxAttempts();

    int passwordResetIdentityMaxAttempts();

    int passwordResetRateLimitWindowSeconds();
}
