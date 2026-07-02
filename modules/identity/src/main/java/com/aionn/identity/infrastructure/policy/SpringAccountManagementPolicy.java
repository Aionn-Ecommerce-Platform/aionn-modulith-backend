package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.AccountManagementPolicy;
import com.aionn.identity.infrastructure.config.properties.AccountManagementProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringAccountManagementPolicy implements AccountManagementPolicy {

    private final AccountManagementProperties properties;

    @Override
    public int getOtpExpirySeconds() {
        return properties.otp().expirySeconds();
    }

    @Override
    public int getOtpMaxAttempts() {
        return properties.otp().maxAttempts();
    }

    @Override
    public int getDeletionGraceDays() {
        return properties.deletion().graceDays();
    }
}
