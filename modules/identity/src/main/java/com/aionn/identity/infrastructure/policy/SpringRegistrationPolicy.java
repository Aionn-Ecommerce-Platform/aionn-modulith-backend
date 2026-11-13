package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.RegistrationPolicy;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringRegistrationPolicy implements RegistrationPolicy {

    private final RegistrationProperties properties;

    @Override
    public int maxVerifyAttempts() {
        return properties.maxVerifyAttempts();
    }

    @Override
    public int resendCooldownSeconds() {
        return properties.resendCooldownSeconds();
    }

    @Override
    public int otpExpirySeconds() {
        return properties.otpExpirySeconds();
    }

    @Override
    public int lockTimeoutSeconds() {
        return properties.lockTimeoutSeconds();
    }

    @Override
    public long sessionExpiresDays() {
        return properties.sessionExpiresDays();
    }

    @Override
    public String defaultCountryCallingCode() {
        return properties.defaultCountryCallingCode();
    }

    @Override
    public boolean isExposeOtpInResponse() {
        return properties.exposeOtpInResponse();
    }

    @Override
    public int ipRateLimitMaxAttempts() {
        return properties.rateLimit().ipMaxAttempts();
    }

    @Override
    public int ipRateLimitWindowSeconds() {
        return properties.rateLimit().ipWindowSeconds();
    }

    @Override
    public int phoneRateLimitMaxAttempts() {
        return properties.rateLimit().phoneMaxAttempts();
    }

    @Override
    public int phoneRateLimitWindowSeconds() {
        return properties.rateLimit().phoneWindowSeconds();
    }

    @Override
    public int verifyIpMaxAttempts() {
        return properties.rateLimit().verifyIpMaxAttempts();
    }

    @Override
    public int verifyIpWindowSeconds() {
        return properties.rateLimit().verifyIpWindowSeconds();
    }

    @Override
    public int resendIpMaxAttempts() {
        return properties.rateLimit().resendIpMaxAttempts();
    }

    @Override
    public int resendIpWindowSeconds() {
        return properties.rateLimit().resendIpWindowSeconds();
    }
}
