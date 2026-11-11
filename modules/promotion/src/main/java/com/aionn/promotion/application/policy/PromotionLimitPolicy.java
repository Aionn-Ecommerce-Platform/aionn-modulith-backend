package com.aionn.promotion.application.policy;

public interface PromotionLimitPolicy {

    int maxListLimit();

    int maxRegistrationLimit();

    int maxActiveFlashSaleLimit();

    int topVouchersLimit();
}
