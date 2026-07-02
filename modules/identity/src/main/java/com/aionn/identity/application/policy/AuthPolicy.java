package com.aionn.identity.application.policy;

public interface AuthPolicy {

    long getSessionExpiresDays();

    int getAccessTokenExpiryMinutes();

    int getMaxFailedLoginAttempts();

    int getLockoutMinutes();

    int getPasswordResetTokenTtlMinutes();
}
