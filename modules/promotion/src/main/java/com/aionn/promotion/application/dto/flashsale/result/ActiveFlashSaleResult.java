package com.aionn.promotion.application.dto.flashsale.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ActiveFlashSaleResult(
                String campaignId,
                String name,
                Instant startDate,
                Instant endDate,
                List<Item> items) {

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
