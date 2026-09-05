package com.aionn.sharedkernel.integration.port.promotion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface FlashSaleQueryPort {

    Map<String, ProductFlashSale> findActiveByProductIds(List<String> productIds);

    Map<String, SkuFlashSale> findActiveBySkuIds(List<String> skuIds);

    void reserve(String orderId, List<Allocation> allocations);

    void release(String orderId);

    List<ActiveFlashSaleCampaign> listActiveCampaigns(int limit);

    record ProductFlashSale(
            String productId,
            String campaignId,
            Instant endAt,
            List<SkuOffer> skuOffers) {

        public BigDecimal lowestSalePrice() {
            return skuOffers.stream()
                    .map(SkuOffer::salePrice)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
        }

        public int totalSaleStock() {
            return skuOffers.stream().mapToInt(SkuOffer::saleStock).sum();
        }

        public int totalSoldCount() {
            return skuOffers.stream().mapToInt(SkuOffer::soldCount).sum();
        }
    }

    record SkuOffer(
            String skuId,
            BigDecimal salePrice,
            String currency,
            int saleStock,
            int soldCount) {
    }

    record SkuFlashSale(
            String skuId,
            String registrationId,
            String campaignId,
            BigDecimal salePrice,
            String currency,
            Instant endAt,
            int remainingStock) {
        public SkuFlashSale(String skuId, String campaignId, BigDecimal salePrice,
                String currency, Instant endAt, int remainingStock) {
            this(skuId, null, campaignId, salePrice, currency, endAt, remainingStock);
        }
    }

    record Allocation(String registrationId, int quantity) {
    }

    final class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String registrationId) {
            super("Flash-sale capacity exceeded for registration " + registrationId);
        }
    }

    record ActiveFlashSaleCampaign(
            String campaignId,
            String name,
            Instant startDate,
            Instant endDate,
            List<String> productIds) {
    }
}
