package com.aionn.promotion.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.promotion.domain.event.PromotionEvents;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.promotion.domain.valueobject.VoucherScope;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class Voucher extends AggregateRoot {

    private final String voucherCode;
    private final String campaignId;
    private final VoucherScope scope;
    private final String merchantId;
    private final Money discountAmount;
    private final int usageLimit;
    private int usedCount;
    private int reservedCount;
    private final Instant validFrom;
    private final Instant validUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    public Voucher(String voucherCode, String campaignId, VoucherScope scope, String merchantId, Money discountAmount,
            int usageLimit, int usedCount, int reservedCount,
            Instant validFrom, Instant validUntil, Instant createdAt, Instant updatedAt) {
        this.voucherCode = voucherCode;
        this.campaignId = campaignId;
        this.scope = scope;
        this.merchantId = merchantId;
        this.discountAmount = discountAmount;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.reservedCount = reservedCount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Voucher issue(String voucherCode, String campaignId, Money discountAmount,
            int usageLimit, Instant validFrom, Instant validUntil) {
        return issue(voucherCode, campaignId, discountAmount, usageLimit, validFrom, validUntil, Clock.systemUTC());
    }

    public static Voucher issue(String voucherCode, String campaignId, Money discountAmount,
            int usageLimit, Instant validFrom, Instant validUntil, Clock clock) {
        Guard.require(usageLimit > 0,
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT, "usageLimit must be > 0"));
        Guard.require(validUntil == null || validFrom == null || validFrom.isBefore(validUntil),
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "validFrom must be before validUntil"));
        Instant now = clock.instant();
        Voucher v = new Voucher(voucherCode, campaignId, VoucherScope.PLATFORM, null, discountAmount, usageLimit, 0, 0,
                validFrom, validUntil, now, now);
        v.registerEvent(new PromotionEvents.VoucherIssued(voucherCode, campaignId,
                discountAmount.amount(), discountAmount.currency(),
                usageLimit, validUntil, now));
        return v;
    }

    public static Voucher issueForShop(String voucherCode, String merchantId, Money discountAmount,
            int usageLimit, Instant validFrom, Instant validUntil) {
        return issueForShop(voucherCode, merchantId, discountAmount, usageLimit, validFrom, validUntil,
                Clock.systemUTC());
    }

    public static Voucher issueForShop(String voucherCode, String merchantId, Money discountAmount,
            int usageLimit, Instant validFrom, Instant validUntil, Clock clock) {
        Guard.notBlank(merchantId, "merchantId");
        Guard.require(usageLimit > 0,
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT, "usageLimit must be > 0"));
        Guard.require(validUntil == null || validFrom == null || validFrom.isBefore(validUntil),
                () -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "validFrom must be before validUntil"));
        Instant now = clock.instant();
        Voucher voucher = new Voucher(voucherCode, null, VoucherScope.SHOP, merchantId, discountAmount,
                usageLimit, 0, 0, validFrom, validUntil, now, now);
        voucher.registerEvent(new PromotionEvents.VoucherIssued(voucherCode, null,
                discountAmount.amount(), discountAmount.currency(), usageLimit, validUntil, now));
        return voucher;
    }

    public boolean appliesToMerchant(String targetMerchantId) {
        return scope == VoucherScope.PLATFORM || merchantId.equals(targetMerchantId);
    }

    public boolean isValidNow(Instant now) {
        if (validUntil != null && now.isAfter(validUntil))
            return false;
        if (validFrom != null && now.isBefore(validFrom))
            return false;
        return true;
    }

    public boolean isUsable(Instant now) {
        return isValidNow(now) && remainingUses() > 0;
    }

    public int remainingUses() {
        return Math.max(usageLimit - usedCount - reservedCount, 0);
    }

    public void claimSlot() {
        claimSlot(Clock.systemUTC());
    }

    public void claimSlot(Clock clock) {
        Instant now = clock.instant();
        Guard.require(isValidNow(now),
                () -> new PromotionException(PromotionErrorCode.VOUCHER_EXPIRED));
        Guard.require(remainingUses() > 0,
                () -> new PromotionException(PromotionErrorCode.VOUCHER_NO_USAGE_LEFT));
        this.updatedAt = now;
    }

    public void reserveSlot() {
        reserveSlot(Clock.systemUTC());
    }

    public void reserveSlot(Clock clock) {
        Instant now = clock.instant();
        Guard.require(isValidNow(now),
                () -> new PromotionException(PromotionErrorCode.VOUCHER_EXPIRED));
        Guard.require(remainingUses() > 0,
                () -> new PromotionException(PromotionErrorCode.VOUCHER_NO_USAGE_LEFT));
        this.reservedCount++;
        this.updatedAt = now;
    }

    public void commitSlot() {
        commitSlot(Clock.systemUTC());
    }

    public void commitSlot(Clock clock) {
        Guard.require(reservedCount > 0,
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_INVALID_STATE,
                        "No reserved voucher slot to commit"));
        this.reservedCount--;
        this.usedCount++;
        this.updatedAt = clock.instant();
    }

    public void releaseSlot() {
        releaseSlot(Clock.systemUTC());
    }

    public void releaseSlot(Clock clock) {
        Guard.require(reservedCount > 0,
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_INVALID_STATE,
                        "No reserved voucher slot to release"));
        this.reservedCount--;
        this.updatedAt = clock.instant();
    }

    public void releaseCommittedSlot(Clock clock) {
        Guard.require(usedCount > 0,
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_INVALID_STATE,
                        "No committed voucher slot to release"));
        this.usedCount--;
        this.updatedAt = clock.instant();
    }

    @Override
    protected String aggregateId() {
        return voucherCode;
    }
}
