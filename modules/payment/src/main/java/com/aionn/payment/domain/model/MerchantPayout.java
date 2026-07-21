package com.aionn.payment.domain.model;

import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import com.aionn.sharedkernel.domain.Guard;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class MerchantPayout {

    private final String payoutId;
    private final String merchantId;
    private final BigDecimal amount;
    private final String currency;
    private PayoutStatus status;
    private final String bankName;
    private final String bankAccountNo;
    private final String bankAccountName;
    private String externalRef;
    private String note;
    private final Instant requestedAt;
    private Instant completedAt;
    private Instant failedAt;
    private String failureReason;
    private long version;

    public MerchantPayout(String payoutId, String merchantId, BigDecimal amount, String currency,
            PayoutStatus status, String bankName, String bankAccountNo, String bankAccountName,
            String externalRef, String note, Instant requestedAt, Instant completedAt,
            Instant failedAt, String failureReason, long version) {
        this.payoutId = payoutId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.bankName = bankName;
        this.bankAccountNo = bankAccountNo;
        this.bankAccountName = bankAccountName;
        this.externalRef = externalRef;
        this.note = note;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
        this.version = version;
    }

    public static MerchantPayout request(String payoutId, String merchantId, BigDecimal amount,
            String currency, String bankName, String bankAccountNo, String bankAccountName, String note, Instant now) {
        Guard.require(amount.signum() > 0,
                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount must be positive"));
        return new MerchantPayout(payoutId, merchantId, amount, currency, PayoutStatus.PENDING,
                bankName, bankAccountNo, bankAccountName, null, note, now, null, null, null, 0);
    }

    public void markProcessing() {
        Guard.require(status == PayoutStatus.PENDING,
                () -> new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Only PENDING payouts can move to PROCESSING"));
        this.status = PayoutStatus.PROCESSING;
    }

    public void markCompleted(String externalRef, Instant now) {
        Guard.require(status == PayoutStatus.PENDING || status == PayoutStatus.PROCESSING,
                () -> new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Cannot complete payout in state " + status));
        this.status = PayoutStatus.COMPLETED;
        this.externalRef = externalRef;
        this.completedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        Guard.require(status == PayoutStatus.PENDING || status == PayoutStatus.PROCESSING,
                () -> new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Cannot fail payout in state " + status));
        this.status = PayoutStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = now;
    }
}
