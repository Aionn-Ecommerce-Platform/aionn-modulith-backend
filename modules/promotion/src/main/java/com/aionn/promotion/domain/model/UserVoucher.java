package com.aionn.promotion.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.promotion.domain.event.PromotionEvents;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class UserVoucher extends AggregateRoot {

    private final String userVoucherId;
    private final String voucherCode;
    private final String userId;
    private UserVoucherStatus status;
    private String reservedOrderId;
    private Money appliedAmount;
    private final Instant claimedAt;
    private Instant reservedAt;
    private Instant reservedExpiresAt;
    private Instant appliedAt;
    private Instant releasedAt;
    private Instant updatedAt;

    public UserVoucher(String userVoucherId, String voucherCode, String userId,
            UserVoucherStatus status, String reservedOrderId, Money appliedAmount,
            Instant claimedAt, Instant reservedAt, Instant reservedExpiresAt,
            Instant appliedAt, Instant releasedAt, Instant updatedAt) {
        this.userVoucherId = userVoucherId;
        this.voucherCode = voucherCode;
        this.userId = userId;
        this.status = status;
        this.reservedOrderId = reservedOrderId;
        this.appliedAmount = appliedAmount;
        this.claimedAt = claimedAt;
        this.reservedAt = reservedAt;
        this.reservedExpiresAt = reservedExpiresAt;
        this.appliedAt = appliedAt;
        this.releasedAt = releasedAt;
        this.updatedAt = updatedAt;
    }

    public static UserVoucher claim(String userVoucherId, String voucherCode, String userId, Clock clock) {
        Instant now = clock.instant();
        UserVoucher uv = new UserVoucher(userVoucherId, voucherCode, userId,
                UserVoucherStatus.CLAIMED, null, null, now, null, null, null, null, now);
        uv.registerEvent(new PromotionEvents.VoucherClaimed(voucherCode, userId, now, now));
        return uv;
    }

    public void reserve(String orderId, Instant expiresAt, Clock clock) {
        ensureTransition(UserVoucherStatus.RESERVED);
        Guard.require(status != UserVoucherStatus.RESERVED || orderId.equals(reservedOrderId),
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_RESERVED_BY_OTHER));
        this.status = UserVoucherStatus.RESERVED;
        this.reservedOrderId = orderId;
        this.reservedAt = clock.instant();
        this.reservedExpiresAt = expiresAt;
        this.updatedAt = reservedAt;
        registerEvent(new PromotionEvents.VoucherReserved(voucherCode, userId, orderId,
                reservedAt, expiresAt, reservedAt));
    }

    public void apply(Money amount, Clock clock) {
        Guard.require(status == UserVoucherStatus.RESERVED,
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_INVALID_STATE,
                        "Voucher must be RESERVED to apply"));
        this.status = UserVoucherStatus.APPLIED;
        this.appliedAmount = amount;
        Instant now = clock.instant();
        this.appliedAt = now;
        this.updatedAt = now;
        registerEvent(new PromotionEvents.VoucherApplied(voucherCode, userId, reservedOrderId,
                amount.amount(), amount.currency(), appliedAt, appliedAt));
    }

    public void release(String reason, Clock clock) {
        Guard.require(status == UserVoucherStatus.RESERVED || status == UserVoucherStatus.APPLIED,
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_INVALID_STATE,
                        "Only RESERVED or APPLIED vouchers can be released"));
        String orderId = reservedOrderId;
        this.status = UserVoucherStatus.RELEASED;
        Instant now = clock.instant();
        this.releasedAt = now;
        this.updatedAt = now;
        this.reservedOrderId = null;
        this.reservedAt = null;
        this.reservedExpiresAt = null;
        registerEvent(new PromotionEvents.VoucherReleased(voucherCode, userId, orderId, reason, now, now));
    }

    public void expire(Clock clock) {
        if (status == UserVoucherStatus.APPLIED)
            return;
        if (status == UserVoucherStatus.EXPIRED)
            return;
        this.status = UserVoucherStatus.EXPIRED;
        this.updatedAt = clock.instant();
    }

    public boolean isReservationExpired(Instant now) {
        return status == UserVoucherStatus.RESERVED && reservedExpiresAt != null
                && now.isAfter(reservedExpiresAt);
    }

    private void ensureTransition(UserVoucherStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new PromotionException(PromotionErrorCode.USER_VOUCHER_INVALID_STATE,
                        "Cannot transition user voucher from " + status + " to " + next));
    }

    @Override
    protected String aggregateId() {
        return userVoucherId;
    }
}
