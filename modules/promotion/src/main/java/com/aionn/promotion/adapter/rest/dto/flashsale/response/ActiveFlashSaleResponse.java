package com.aionn.promotion.adapter.rest.dto.flashsale.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ActiveFlashSaleResponse(
        String campaignId,
        String name,
        Instant startDate,
        Instant endDate,
        List<Item> items,
        int page,
        int size,
        long totalItems,
        long totalSold,
        long totalStock,
        BigDecimal minSalePrice,
        BigDecimal maxSalePrice) {

    public record Item(
            String registrationId,
            String productId,
            String skuId,
            String merchantId,
            BigDecimal salePrice,
            String currency,
            int saleStock,
            int soldCount) {
    }
}
