package com.aionn.payment.infrastructure.policy;

import com.aionn.payment.application.policy.PaymentDefaultsPolicy;
import com.aionn.payment.infrastructure.config.properties.PaymentDefaultsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPaymentDefaultsPolicy implements PaymentDefaultsPolicy {

    private static final String FALLBACK_CURRENCY = "VND";

    private final PaymentDefaultsProperties properties;

    @Override
    public String defaultCurrency() {
        String currency = properties.currency();
        return (currency == null || currency.isBlank()) ? FALLBACK_CURRENCY : currency.trim().toUpperCase();
    }
}
