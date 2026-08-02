package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.application.policy.CarrierWebhookSecurityPolicy;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GhnCarrierWebhookSecurityPolicy implements CarrierWebhookSecurityPolicy {

    private final GhnProperties properties;

    @Override
    public boolean isAuthorized(String providedSecret) {
        String expectedSecret = properties.webhookSecret();
        return expectedSecret == null
                || expectedSecret.isBlank()
                || expectedSecret.equals(providedSecret);
    }
}
