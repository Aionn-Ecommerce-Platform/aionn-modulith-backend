package com.aionn.promotion.infrastructure.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Prevents exposing a campaign feature that checkout does not yet consume. */
@Component
public class FlashSaleReadinessValidator implements SmartInitializingSingleton {

    private final boolean enabled;

    public FlashSaleReadinessValidator(
            @Value("${promotion.flash-sale.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (enabled) {
            throw new IllegalStateException(
                    "promotion.flash-sale.enabled cannot be enabled until ordering consumes flash-sale pricing");
        }
    }
}
