package com.aionn.payment.domain.model;

import com.aionn.payment.domain.event.PaymentEvents;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.PaymentStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static Payment newPayment() {
        return Payment.initiate("p1", "o1", "u1", "m1",
                Money.of(new BigDecimal("100"), "VND"),
                PaymentGatewayKind.STRIPE, "idem-1", FIXED_NOW);
    }

    @Test
    void initiateCreatesIntiatedPaymentAndEmitsEvent() {
        Payment p = newPayment();

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(p.getPaymentId()).isEqualTo("p1");
        assertThat(p.getOrderId()).isEqualTo("o1");
        assertThat(p.getRefundedAmount().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.peekEvents())
                .anyMatch(env -> env.payload() instanceof PaymentEvents.PaymentInitiated);
    }

    @Test
    void initiateRequiresIdempotencyKey() {
        assertThatThrownBy(() -> Payment.initiate("p1", "o1", "u1", "m1",
                Money.of(new BigDecimal("100"), "VND"),
                PaymentGatewayKind.VNPAY, " ", FIXED_NOW))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void markPaidTransitionsToPaidAndRecordsEvent() {
        Payment p = newPayment();
        p.pullEvents();

        p.markPaid("txn-100", FIXED_NOW);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(p.getTransactionNo()).isEqualTo("txn-100");
        assertThat(p.getPaidAt()).isNotNull();
        assertThat(p.peekEvents())
                .anyMatch(env -> env.payload() instanceof PaymentEvents.PaymentProcessed);
    }

    @Test
    void markFailedTransitionsToFailed() {
        Payment p = newPayment();

        p.markFailed("E1", "boom", FIXED_NOW);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(p.getErrorCode()).isEqualTo("E1");
        assertThat(p.getErrorReason()).isEqualTo("boom");
    }

    @Test
    void refundFromInitiatedRejected() {
        Payment p = newPayment();

        assertThatThrownBy(() -> p.refund("r1",
                Money.of(new BigDecimal("10"), "VND"), "x", FIXED_NOW))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_PAID.getCode());
    }

    @Test
    void refundFullAmountTransitionsToRefunded() {
        Payment p = newPayment();
        p.markPaid("txn-100", FIXED_NOW);

        p.refund("r1", Money.of(new BigDecimal("100"), "VND"), "duplicate", FIXED_NOW);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(p.getRefundedAmount().amount()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void refundPartialKeepsPaidStatus() {
        Payment p = newPayment();
        p.markPaid("txn-100", FIXED_NOW);

        p.refund("r1", Money.of(new BigDecimal("40"), "VND"), "partial", FIXED_NOW);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(p.getRefundedAmount().amount()).isEqualByComparingTo(new BigDecimal("40"));
    }

    @Test
    void attachInvoiceWhenNotPaidThrows() {
        Payment p = newPayment();

        assertThatThrownBy(() -> p.attachInvoice("https://invoice.example/p1.pdf", FIXED_NOW))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_PAID.getCode());
    }
}
