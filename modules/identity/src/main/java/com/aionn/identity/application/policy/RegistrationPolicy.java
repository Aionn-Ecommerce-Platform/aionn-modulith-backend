package com.aionn.identity.application.policy;

public interface RegistrationPolicy {

    int maxVerifyAttempts();

    int resendCooldownSeconds();

    int otpExpirySeconds();

    int lockTimeoutSeconds();

    long sessionExpiresDays();

    String defaultCountryCallingCode();

    boolean isExposeOtpInResponse();

    int ipRateLimitMaxAttempts();

    int ipRateLimitWindowSeconds();

    int phoneRateLimitMaxAttempts();

    int phoneRateLimitWindowSeconds();

    int verifyIpMaxAttempts();

    int verifyIpWindowSeconds();

    int resendIpMaxAttempts();

    int resendIpWindowSeconds();
}
