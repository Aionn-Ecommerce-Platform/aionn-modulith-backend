package com.aionn.payment.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.payment.domain.event.PaymentEvents;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.PaymentStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class Payment extends AggregateRoot {

        private final String paymentId;
        private final String orderId;
        private final String userId;
        private final String paymentMethodId;
        private final Money amount;
        private final PaymentGatewayKind gateway;
        private final String idempotencyKey;
        private PaymentStatus status;
        private String transactionNo;
        private String invoiceUrl;
        private String errorCode;
        private String errorReason;
        private Money refundedAmount;
        private final Instant createdAt;
        private Instant updatedAt;
        private Instant paidAt;
        private Instant failedAt;

        public Payment(
                        String paymentId,
                        String orderId,
                        String userId,
                        String paymentMethodId,
                        Money amount,
                        PaymentGatewayKind gateway,
                        String idempotencyKey,
                        PaymentStatus status,
                        String transactionNo,
                        String invoiceUrl,
                        String errorCode,
                        String errorReason,
                        Money refundedAmount,
                        Instant createdAt,
                        Instant updatedAt,
                        Instant paidAt,
                        Instant failedAt) {
                this.paymentId = paymentId;
                this.orderId = orderId;
                this.userId = userId;
                this.paymentMethodId = paymentMethodId;
                this.amount = amount;
                this.gateway = gateway;
                this.idempotencyKey = idempotencyKey;
                this.status = status;
                this.transactionNo = transactionNo;
                this.invoiceUrl = invoiceUrl;
                this.errorCode = errorCode;
                this.errorReason = errorReason;
                this.refundedAmount = refundedAmount == null ? Money.zero(amount.currency()) : refundedAmount;
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
                this.paidAt = paidAt;
                this.failedAt = failedAt;
        }

        public static Payment initiate(
                        String paymentId,
                        String orderId,
                        String userId,
                        String paymentMethodId,
                        Money amount,
                        PaymentGatewayKind gateway,
                        String idempotencyKey,
                        Instant now) {
                Guard.require(idempotencyKey != null && !idempotencyKey.isBlank(),
                                () -> new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                                                "idempotencyKey required"));
                Payment p = new Payment(paymentId, orderId, userId, paymentMethodId, amount, gateway,
                                idempotencyKey, PaymentStatus.INITIATED, null, null, null, null,
                                Money.zero(amount.currency()), now, now, null, null);
                p.registerEvent(new PaymentEvents.PaymentInitiated(paymentId, orderId, amount.amount(), amount.currency(),
                                gateway.name(), paymentMethodId, idempotencyKey, now));
                return p;
        }

        public void markPaid(String transactionNo, Instant now) {
                ensureTransition(PaymentStatus.PAID);
                this.status = PaymentStatus.PAID;
                this.transactionNo = transactionNo;
                this.paidAt = now;
                this.updatedAt = now;
                registerEvent(new PaymentEvents.PaymentProcessed(paymentId, orderId, transactionNo, gateway.name(),
                                amount.amount(), amount.currency(), paidAt, now));
        }

        public void markFailed(String errorCode, String reason, Instant now) {
                ensureTransition(PaymentStatus.FAILED);
                this.status = PaymentStatus.FAILED;
                this.errorCode = errorCode;
                this.errorReason = reason;
                this.failedAt = now;
                this.updatedAt = now;
                registerEvent(new PaymentEvents.PaymentFailed(paymentId, orderId, errorCode, reason, now));
        }

        public void refund(String refundId, Money refundAmount, String reason, Instant now) {
                Guard.require(status == PaymentStatus.PAID,
                                () -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_PAID));
                Money remaining = amount.subtract(refundedAmount);
                Guard.require(!refundAmount.isGreaterThan(remaining),
                                () -> new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_EXCEEDED,
                                                "Requested " + refundAmount.amount() + " but only " + remaining.amount()
                                                                + " remains"));
                this.refundedAmount = refundedAmount.add(refundAmount);
                if (refundedAmount.amount().compareTo(amount.amount()) >= 0) {
                        this.status = PaymentStatus.REFUNDED;
                }
                this.updatedAt = now;
                registerEvent(new PaymentEvents.PaymentRefunded(paymentId, orderId, refundId, refundAmount.amount(),
                                refundAmount.currency(), reason, now));
        }

        public void attachInvoice(String invoiceUrl, Instant now) {
                Guard.require(status == PaymentStatus.PAID || status == PaymentStatus.REFUNDED,
                                () -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_PAID,
                                                "Invoice can only be attached to a PAID payment"));
                this.invoiceUrl = invoiceUrl;
                this.updatedAt = now;
                registerEvent(new PaymentEvents.InvoiceGenerated(paymentId, orderId, invoiceUrl, now, now));
        }

        private void ensureTransition(PaymentStatus next) {
                Guard.require(status.canTransitionTo(next),
                                () -> new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                                                "Cannot transition from " + status + " to " + next));
        }

        @Override
        protected String aggregateId() {
                return paymentId;
        }
}
