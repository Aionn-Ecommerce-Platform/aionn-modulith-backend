package com.aionn.identity.application.policy;

public interface RegistrationPolicy {

    int getMaxVerifyAttempts();

    int getResendCooldownSeconds();

    int getOtpExpirySeconds();

    int getLockTimeoutSeconds();

    long getSessionExpiresDays();

    String getDefaultCountryCallingCode();

    boolean isExposeOtpInResponse();

    int getIpRateLimitMaxAttempts();

    int getIpRateLimitWindowSeconds();

    int getPhoneRateLimitMaxAttempts();

    int getPhoneRateLimitWindowSeconds();
}
