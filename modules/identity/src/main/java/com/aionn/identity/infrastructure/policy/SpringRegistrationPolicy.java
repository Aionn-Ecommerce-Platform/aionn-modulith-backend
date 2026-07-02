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
    public int getMaxVerifyAttempts() {
        return properties.maxVerifyAttempts();
    }

    @Override
    public int getResendCooldownSeconds() {
        return properties.resendCooldownSeconds();
    }

    @Override
    public int getOtpExpirySeconds() {
        return properties.otpExpirySeconds();
    }

    @Override
    public int getLockTimeoutSeconds() {
        return properties.lockTimeoutSeconds();
    }

    @Override
    public long getSessionExpiresDays() {
        return properties.sessionExpiresDays();
    }

    @Override
    public String getDefaultCountryCallingCode() {
        return properties.defaultCountryCallingCode();
    }

    @Override
    public boolean isExposeOtpInResponse() {
        return !properties.twilio().enabled();
    }

    @Override
    public int getIpRateLimitMaxAttempts() {
        return properties.rateLimit().ipMaxAttempts();
    }

    @Override
    public int getIpRateLimitWindowSeconds() {
        return properties.rateLimit().ipWindowSeconds();
    }

    @Override
    public int getPhoneRateLimitMaxAttempts() {
        return properties.rateLimit().phoneMaxAttempts();
    }

    @Override
    public int getPhoneRateLimitWindowSeconds() {
        return properties.rateLimit().phoneWindowSeconds();
    }
}
