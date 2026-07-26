package com.aionn.promotion.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PromotionEvents {

    private PromotionEvents() {
    }

    public record PromotionCampaignCreated(
            String campaignId, String name, BigDecimal budget, String currency,
            Instant startDate, Instant endDate, String createdBy, Instant occurredAt) implements PromotionEvent {
    }

    public record PromotionCampaignActivated(
            String campaignId, Instant occurredAt) implements PromotionEvent {
    }

    public record PromotionCampaignEnded(
            String campaignId, Instant endedAt, Instant occurredAt) implements PromotionEvent {
    }

    public record PromotionCampaignCancelled(
            String campaignId, String reason, Instant cancelledAt, Instant occurredAt) implements PromotionEvent {
    }

    public record VoucherIssued(
            String voucherCode, String campaignId, BigDecimal discountAmount, String currency,
            int usageLimit, Instant validUntil, Instant occurredAt) implements PromotionEvent {
    }

    public record VoucherClaimed(
            String voucherCode, String userId, Instant claimedAt, Instant occurredAt) implements PromotionEvent {
    }

    public record VoucherReserved(
            String voucherCode, String userId, String orderId,
            Instant reservedAt, Instant expiresAt, Instant occurredAt) implements PromotionEvent {
    }

    public record VoucherApplied(
            String voucherCode, String userId, String orderId,
            BigDecimal appliedAmount, String currency, Instant appliedAt, Instant occurredAt)
            implements PromotionEvent {
    }

    public record VoucherReleased(
            String voucherCode, String userId, String orderId, String reason,
            Instant releasedAt, Instant occurredAt) implements PromotionEvent {
    }

    public record PromotionConditionConfigured(
            String campaignId, BigDecimal minOrderValue, List<String> applicableCategories,
            Integer maxClaimsPerUser, Integer maxUsesPerVoucher,
            Instant configuredAt, Instant occurredAt) implements PromotionEvent {
    }

    public record FlashSaleRegistered(
            String registrationId, String campaignId, String merchantId,
            String productId, String skuId,
            BigDecimal salePrice, String currency, int saleStock,
            Instant occurredAt) implements PromotionEvent {
    }

    public record FlashSaleApproved(
            String registrationId, String campaignId, String skuId,
            String adminId, Instant occurredAt) implements PromotionEvent {
    }

    public record FlashSaleRejected(
            String registrationId, String campaignId, String skuId,
            String adminId, String reason, Instant occurredAt) implements PromotionEvent {
    }

    public record FlashSaleCancelled(
            String registrationId, String campaignId, String skuId,
            Instant occurredAt) implements PromotionEvent {
    }
}

