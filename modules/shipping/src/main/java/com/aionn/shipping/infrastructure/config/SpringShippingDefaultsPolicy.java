package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.application.policy.ShippingDefaultsPolicy;
import com.aionn.shipping.infrastructure.config.properties.ShippingDefaultsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringShippingDefaultsPolicy implements ShippingDefaultsPolicy {

    private static final String FALLBACK_CURRENCY = "VND";

    private final ShippingDefaultsProperties properties;

    @Override
    public String defaultCurrency() {
        String currency = properties.currency();
        return (currency == null || currency.isBlank()) ? FALLBACK_CURRENCY : currency.trim().toUpperCase();
    }

    @Override
    public int defaultBaseWeightGram() {
        return properties.baseWeightGram();
    }

    @Override
    public int defaultStepWeightGram() {
        return properties.stepWeightGram();
    }
}
