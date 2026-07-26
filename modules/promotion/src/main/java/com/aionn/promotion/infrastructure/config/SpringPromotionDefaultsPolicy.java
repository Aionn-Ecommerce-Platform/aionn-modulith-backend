package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.application.policy.PromotionDefaultsPolicy;
import com.aionn.promotion.infrastructure.config.properties.PromotionDefaultsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPromotionDefaultsPolicy implements PromotionDefaultsPolicy {

    private final PromotionDefaultsProperties properties;

    @Override
    public String defaultCurrency() {
        String currency = properties.currency();
        return (currency == null || currency.isBlank()) ? "VND" : currency.trim().toUpperCase();
    }
}
