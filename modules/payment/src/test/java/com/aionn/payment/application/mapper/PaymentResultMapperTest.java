package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.ledger.result.LedgerResult;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.domain.valueobject.LedgerEntryType;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResultMapperTest {

    private final PaymentResultMapper mapper = new PaymentResultMapper();

    @Test
    void mapsInitiatedPaymentToResult() {
        Payment payment = Payment.initiate("p1", "o1", "u1", "m1",
                Money.of(new BigDecimal("150"), "VND"),
                PaymentGatewayKind.VNPAY, "idem-1", java.time.Instant.now());

        PaymentResult result = mapper.toResult(payment);

        assertThat(result.paymentId()).isEqualTo("p1");
        assertThat(result.orderId()).isEqualTo("o1");
        assertThat(result.userId()).isEqualTo("u1");
        assertThat(result.paymentMethodId()).isEqualTo("m1");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.gateway()).isEqualTo("VNPAY");
        assertThat(result.status()).isEqualTo("INITIATED");
        assertThat(result.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.redirectUrl()).isNull();
    }

    @Test
    void mapsPaidPaymentExposesTransactionNo() {
        Payment payment = Payment.initiate("p2", "o2", "u2", null,
                Money.of(new BigDecimal("99"), "USD"),
                PaymentGatewayKind.STRIPE, "idem-2", java.time.Instant.now());
        payment.markPaid("txn-42", java.time.Instant.now());

        PaymentResult result = mapper.toResult(payment);

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.transactionNo()).isEqualTo("txn-42");
        assertThat(result.paidAt()).isNotNull();
    }

    @Test
    void mapsPaymentMethod() {
        PaymentMethod m = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", java.time.Instant.now());

        PaymentMethodResult result = mapper.toResult(m);

        assertThat(result.methodId()).isEqualTo("m1");
        assertThat(result.userId()).isEqualTo("u1");
        assertThat(result.provider()).isEqualTo("stripe");
        assertThat(result.last4Digits()).isEqualTo("4242");
        assertThat(result.status()).isEqualTo("LINKED");
    }

    @Test
    void mapsTransactionLedger() {
        TransactionLedger ledger = TransactionLedger.record("l1", "p1",
                Money.of(new BigDecimal("100"), "VND"),
                LedgerEntryType.CREDIT, "STRIPE", "txn-1", java.time.Instant.now());

        LedgerResult result = mapper.toResult(ledger);

        assertThat(result.ledgerId()).isEqualTo("l1");
        assertThat(result.paymentId()).isEqualTo("p1");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.type()).isEqualTo("CREDIT");
        assertThat(result.gateway()).isEqualTo("STRIPE");
        assertThat(result.gatewayTransactionNo()).isEqualTo("txn-1");
    }
}
