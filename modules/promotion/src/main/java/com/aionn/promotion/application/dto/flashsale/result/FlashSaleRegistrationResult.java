package com.aionn.promotion.application.dto.flashsale.result;

import java.math.BigDecimal;
import java.time.Instant;

public record FlashSaleRegistrationResult(
        String registrationId,
        String campaignId,
        String merchantId,
        String productId,
        String skuId,
        BigDecimal salePrice,
        String currency,
        int saleStock,
        int soldCount,
        String status,
        String rejectReason,
        Instant submittedAt,
        Instant decidedAt,
        String decidedBy,
        Instant updatedAt) {
}
