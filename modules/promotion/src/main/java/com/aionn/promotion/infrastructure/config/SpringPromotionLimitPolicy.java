package com.aionn.promotion.infrastructure.config;

import com.aionn.promotion.application.policy.PromotionLimitPolicy;
import com.aionn.promotion.infrastructure.config.properties.PromotionLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPromotionLimitPolicy implements PromotionLimitPolicy {

    private final PromotionLimitProperties properties;

    @Override
    public int maxListLimit() {
        return properties.maxListLimit();
    }

    @Override
    public int maxRegistrationLimit() {
        return properties.maxRegistrationLimit();
    }

    @Override
    public int maxActiveFlashSaleLimit() {
        return properties.maxActiveFlashSaleLimit();
    }

    @Override
    public int topVouchersLimit() {
        return properties.topVouchersLimit();
    }
}
