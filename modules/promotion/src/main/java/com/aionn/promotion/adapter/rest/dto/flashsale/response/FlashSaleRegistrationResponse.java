package com.aionn.promotion.adapter.rest.dto.flashsale.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FlashSaleRegistrationResponse(
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
