package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.event.PromotionEvents;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.sharedkernel.domain.vo.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/**
 * One SKU registered by a merchant into a flash-sale campaign. Approval is
 * controlled by admins so the platform can enforce minimum discount rules and
 * keep the storefront curated.
 */
@Getter
public class FlashSaleRegistration extends AggregateRoot {

    /** Minimum discount enforced on approval to keep flash sale meaningful. */
    public static final BigDecimal MIN_DISCOUNT_PERCENT = new BigDecimal("10");

    private final String registrationId;
    private final String campaignId;
    private final String merchantId;
    private final String productId;
    private final String skuId;
    private Money salePrice;
    private int saleStock;
    private int soldCount;
    private FlashSaleRegistrationStatus status;
    private String rejectReason;
    private final Instant submittedAt;
    private Instant decidedAt;
    private String decidedBy;
    private Instant updatedAt;

    public FlashSaleRegistration(
            String registrationId,
            String campaignId,
            String merchantId,
            String productId,
            String skuId,
            Money salePrice,
            int saleStock,
            int soldCount,
            FlashSaleRegistrationStatus status,
            String rejectReason,
            Instant submittedAt,
            Instant decidedAt,
            String decidedBy,
            Instant updatedAt) {
        this.registrationId = registrationId;
        this.campaignId = campaignId;
        this.merchantId = merchantId;
        this.productId = productId;
        this.skuId = skuId;
        this.salePrice = salePrice;
        this.saleStock = saleStock;
        this.soldCount = soldCount;
        this.status = status;
        this.rejectReason = rejectReason;
        this.submittedAt = submittedAt;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
        this.updatedAt = updatedAt;
    }

    public static FlashSaleRegistration submit(
            String registrationId,
            String campaignId,
            String merchantId,
            String productId,
            String skuId,
            Money salePrice,
            int saleStock,
            Clock clock) {
        Guard.notBlank(campaignId, "campaignId");
        Guard.notBlank(merchantId, "merchantId");
        Guard.notBlank(productId, "productId");
        Guard.notBlank(skuId, "skuId");
        Guard.positive(saleStock, "saleStock");
        if (salePrice == null || !salePrice.isPositive()) {
            throw new PromotionException(PromotionErrorCode.INVALID_ARGUMENT, "salePrice must be positive");
        }
        Instant now = clock.instant();
        FlashSaleRegistration reg = new FlashSaleRegistration(
                registrationId, campaignId, merchantId, productId, skuId,
                salePrice, saleStock, 0,
                FlashSaleRegistrationStatus.PENDING, null,
                now, null, null, now);
        reg.registerEvent(new PromotionEvents.FlashSaleRegistered(
                registrationId, campaignId, merchantId, productId, skuId,
                salePrice.amount(), salePrice.currency(), saleStock, now));
        return reg;
    }

    /**
     * Admin approval â€” enforces the platform minimum-discount rule against the
     * regular variant price. {@code variantPrice} comes from catalog and must
     * be in the same currency as {@code salePrice}.
     */

    public void approve(String adminId, Money variantPrice, Clock clock) {
        ensureTransitionAllowed(FlashSaleRegistrationStatus.APPROVED);
        Guard.notBlank(adminId, "adminId");
        if (variantPrice != null && variantPrice.isPositive()) {
            BigDecimal discountPercent = BigDecimal.ONE
                    .subtract(salePrice.amount().divide(variantPrice.amount(), 4, java.math.RoundingMode.HALF_UP))
                    .multiply(BigDecimal.valueOf(100));
            Guard.require(discountPercent.compareTo(MIN_DISCOUNT_PERCENT) >= 0,
                    () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                            "Sale price must be at least " + MIN_DISCOUNT_PERCENT + "% lower than regular price"));
        }
        Instant now = clock.instant();
        this.status = FlashSaleRegistrationStatus.APPROVED;
        this.decidedAt = now;
        this.decidedBy = adminId;
        this.updatedAt = now;
        registerEvent(new PromotionEvents.FlashSaleApproved(registrationId, campaignId, skuId, adminId, now));
    }

    public void reject(String adminId, String reason, Clock clock) {
        ensureTransitionAllowed(FlashSaleRegistrationStatus.REJECTED);
        Guard.notBlank(adminId, "adminId");
        Instant now = clock.instant();
        this.status = FlashSaleRegistrationStatus.REJECTED;
        this.rejectReason = reason;
        this.decidedAt = now;
        this.decidedBy = adminId;
        this.updatedAt = now;
        registerEvent(new PromotionEvents.FlashSaleRejected(registrationId, campaignId, skuId, adminId, reason, now));
    }

    public void cancel(String requesterMerchantId, Clock clock) {
        Guard.require(merchantId.equals(requesterMerchantId),
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "Only owning merchant may cancel registration"));
        ensureTransitionAllowed(FlashSaleRegistrationStatus.CANCELLED);
        this.status = FlashSaleRegistrationStatus.CANCELLED;
        this.updatedAt = clock.instant();
    }

    public void consumeStock(int qty, Clock clock) {
        Guard.positive(qty, "qty");
        Guard.require(status == FlashSaleRegistrationStatus.APPROVED,
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "Cannot consume stock on non-approved registration"));
        Guard.require(soldCount + qty <= saleStock,
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "Insufficient flash-sale stock"));
        this.soldCount += qty;
        this.updatedAt = clock.instant();
    }

    public boolean hasStockLeft() {
        return soldCount < saleStock;
    }

    private void ensureTransitionAllowed(FlashSaleRegistrationStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "Cannot transition flash-sale registration from " + status + " to " + next));
    }

    @Override
    protected String aggregateId() {
        return registrationId;
    }
}
