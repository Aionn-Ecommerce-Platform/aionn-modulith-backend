package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.application.policy.CarrierWebhookSecurityPolicy;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class GhnCarrierWebhookSecurityPolicy implements CarrierWebhookSecurityPolicy {

    private final GhnProperties properties;

    @Override
    public boolean isAuthorized(String providedSecret) {
        String expectedSecret = properties.webhookSecret();
        if (expectedSecret == null || expectedSecret.isBlank()
                || providedSecret == null || providedSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8));
    }
}
