package com.aionn.payment.domain.model;

import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.sharedkernel.domain.Guard;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class MerchantBalance {

    private final String merchantId;
    private final String currency;
    private BigDecimal pending;
    private BigDecimal available;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public MerchantBalance(String merchantId, String currency, BigDecimal pending, BigDecimal available,
            long version, Instant createdAt, Instant updatedAt) {
        this.merchantId = merchantId;
        this.currency = currency;
        this.pending = pending;
        this.available = available;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MerchantBalance empty(String merchantId, String currency, Instant now) {
        return new MerchantBalance(merchantId, currency, BigDecimal.ZERO, BigDecimal.ZERO, 0, now, now);
    }

    public void addPending(BigDecimal amount, Instant now) {
        Guard.require(amount.signum() > 0,
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount must be positive"));
        this.pending = this.pending.add(amount);
        this.updatedAt = now;
    }

    public void moveToAvailable(BigDecimal amount, Instant now) {
        Guard.require(amount.signum() > 0,
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount must be positive"));
        Guard.require(this.pending.compareTo(amount) >= 0,
                () -> new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_EXCEEDED,
                        "pending balance " + pending + " is less than " + amount));
        this.pending = this.pending.subtract(amount);
        this.available = this.available.add(amount);
        this.updatedAt = now;
    }

    public void reversePending(BigDecimal amount, Instant now) {
        Guard.require(amount.signum() > 0,
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount must be positive"));
        Guard.require(this.pending.compareTo(amount) >= 0,
                () -> new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_EXCEEDED,
                        "pending balance " + pending + " is less than " + amount));
        this.pending = this.pending.subtract(amount);
        this.updatedAt = now;
    }

    public void debitAvailable(BigDecimal amount, Instant now) {
        Guard.require(amount.signum() > 0,
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount must be positive"));
        Guard.require(this.available.compareTo(amount) >= 0,
                () -> new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_EXCEEDED,
                        "available balance " + available + " is less than " + amount));
        this.available = this.available.subtract(amount);
        this.updatedAt = now;
    }

    public void creditAvailable(BigDecimal amount, Instant now) {
        Guard.require(amount.signum() > 0,
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount must be positive"));
        this.available = this.available.add(amount);
        this.updatedAt = now;
    }
}
