package com.aionn.promotion.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FlashSaleReadinessValidatorTest {

    @Test
    void disabledFeatureIsSafe() {
        assertDoesNotThrow(() -> new FlashSaleReadinessValidator(false).afterSingletonsInstantiated());
    }

    @Test
    void enablingUnwiredFeatureFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new FlashSaleReadinessValidator(true).afterSingletonsInstantiated());
    }
}
