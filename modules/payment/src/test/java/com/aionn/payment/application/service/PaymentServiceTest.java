package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.out.InvoiceStorage;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.application.port.out.PaymentPersistencePort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.application.port.out.TransactionLedgerPersistencePort;
import com.aionn.payment.application.port.out.integration.PaymentIntegrationEventPublisherPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.PaymentStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentPersistencePort paymentRepository;
    @Mock
    private PaymentMethodPersistencePort paymentMethodRepository;
    @Mock
    private TransactionLedgerPersistencePort ledgerRepository;
    @Mock
    private PaymentProviderRouter providerRouter;
    @Mock
    private InvoiceStorage invoiceStorage;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private PaymentIntegrationEventPublisherPort integrationEventPublisher;
    @Mock
    private OrderQueryPort orderQueryPort;
    @Mock
    private PaymentProviderClient providerClient;

    private final Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);

    private PaymentResultMapper mapper;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        mapper = new PaymentResultMapper();
        service = new PaymentService(
                paymentRepository,
                paymentMethodRepository,
                ledgerRepository,
                providerRouter,
                invoiceStorage,
                mapper,
                eventPublisher,
                integrationEventPublisher,
                orderQueryPort,
                clock);
    }

    @Test
    void initiateReturnsExistingWhenIdempotencyKeyMatches() {
        Payment existing = Payment.initiate("p1", "o1", "u1", null,
                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-1", fixedInstant);
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentResult result = service.initiate(new InitiatePaymentCommand(
                "o1", "u1", null, new BigDecimal("100"), "VND",
                PaymentGatewayKind.STRIPE, "idem-1"));

        assertThat(result.paymentId()).isEqualTo("p1");
        verify(paymentRepository, never()).save(any());
        verify(providerRouter, never()).route(any());
    }

    @Test
    void initiateAsyncReturnsRedirectUrl() {
        when(paymentRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
        when(orderQueryPort.findOrderSummary("o2")).thenReturn(Optional.empty());
        when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                false, null, "https://vnpay.example/redirect", null, null));

        PaymentResult result = service.initiate(new InitiatePaymentCommand(
                "o2", "u2", null, new BigDecimal("100"), "VND",
                PaymentGatewayKind.VNPAY, "idem-2"));

        assertThat(result.status()).isEqualTo("INITIATED");
        assertThat(result.redirectUrl()).isEqualTo("https://vnpay.example/redirect");
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void initiateRejectsUnverifiedMethod() {
        PaymentMethod linked = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok", fixedInstant);
        when(paymentRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(paymentMethodRepository.findById("m1")).thenReturn(Optional.of(linked));

        assertThatThrownBy(() -> service.initiate(new InitiatePaymentCommand(
                "o3", "u1", "m1", new BigDecimal("50"), "VND",
                PaymentGatewayKind.STRIPE, "idem-3")))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_NOT_VERIFIED.getCode());
    }

    @Test
    void confirmMarksPaidAndPublishesIntegrationEvent() {
        Payment payment = Payment.initiate("p4", "o4", "u4", null,
                Money.of(new BigDecimal("75"), "VND"), PaymentGatewayKind.STRIPE, "idem-4", fixedInstant);
        when(paymentRepository.findById("p4")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceStorage.storeInvoiceUrl("p4", "o4")).thenReturn("https://invoice.example/p4.pdf");

        PaymentResult result = service.confirm(new ConfirmPaymentCommand("p4", "txn-4"));

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.transactionNo()).isEqualTo("txn-4");
        assertThat(result.invoiceUrl()).isEqualTo("https://invoice.example/p4.pdf");
        verify(ledgerRepository).save(any());
        verify(integrationEventPublisher).publishPaymentCaptured(eq("p4"), eq("o4"), eq("txn-4"),
                any(BigDecimal.class), eq("VND"));
    }

    @Test
    void failMarksFailedAndPublishesEvent() {
        Payment payment = Payment.initiate("p5", "o5", "u5", null,
                Money.of(new BigDecimal("75"), "VND"), PaymentGatewayKind.STRIPE, "idem-5", fixedInstant);
        when(paymentRepository.findById("p5")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResult result = service.fail(new FailPaymentCommand("p5", "E1", "boom"));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED.name());
        verify(integrationEventPublisher).publishPaymentFailed("p5", "o5", "E1", "boom");
    }

    @Test
    void refundCallsProviderAndPersistsLedger() {
        Payment payment = Payment.initiate("p6", "o6", "u6", null,
                Money.of(new BigDecimal("80"), "VND"), PaymentGatewayKind.STRIPE, "idem-6", fixedInstant);
        payment.markPaid("txn-6", fixedInstant);
        when(paymentRepository.findById("p6")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(providerClient.refund(any())).thenReturn(
                new PaymentProviderClient.Refund(true, "rf-1", null));

        PaymentResult result = service.refund(new RefundPaymentCommand(
                "p6", new BigDecimal("80"), "VND", "duplicate"));

        assertThat(result.status()).isEqualTo("REFUNDED");
        verify(ledgerRepository).save(any());
        verify(integrationEventPublisher).publishPaymentRefunded(eq("p6"), eq("o6"),
                eq("rf-1"), any(BigDecimal.class), eq("VND"), eq("duplicate"));
    }

    @Test
    void refundDeclinedFromProviderThrows() {
        Payment payment = Payment.initiate("p7", "o7", "u7", null,
                Money.of(new BigDecimal("50"), "VND"), PaymentGatewayKind.STRIPE, "idem-7", fixedInstant);
        payment.markPaid("txn-7", fixedInstant);
        when(paymentRepository.findById("p7")).thenReturn(Optional.of(payment));
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(providerClient.refund(any())).thenReturn(
                new PaymentProviderClient.Refund(false, null, "no-funds"));

        assertThatThrownBy(() -> service.refund(new RefundPaymentCommand(
                "p7", new BigDecimal("10"), "VND", "x")))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }

    @Test
    void getForUserOtherUserThrowsNotFound() {
        Payment payment = Payment.initiate("p8", "o8", "u8", null,
                Money.of(new BigDecimal("50"), "VND"), PaymentGatewayKind.STRIPE, "idem-8", fixedInstant);
        when(paymentRepository.findById("p8")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.getForUser("p8", "OTHER"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode());
    }

    @Test
    void requiredMissingThrowsNotFound() {
        when(paymentRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode());
    }

    @Test
    void initiateCapturedDirectly() {
        when(paymentRepository.findByIdempotencyKey("idem-captured")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(orderQueryPort.findOrderSummary("o-cap")).thenReturn(Optional.of(new com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort.OrderSummary("o-cap", "m-cap", new BigDecimal("100"), "VND")));
        when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                true, "txn-cap", null, null, null));
        when(paymentRepository.findById(any())).thenReturn(Optional.of(Payment.initiate("p-cap", "o-cap", "u1", null, Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-captured", fixedInstant)));

        PaymentResult result = service.initiate(new InitiatePaymentCommand(
                "o-cap", "u1", null, new BigDecimal("100"), "VND",
                PaymentGatewayKind.STRIPE, "idem-captured"));

        assertThat(result.status()).isEqualTo("PAID");
    }

    @Test
    void initiateDeclineCodeDirectly() {
        when(paymentRepository.findByIdempotencyKey("idem-decline")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
        when(orderQueryPort.findOrderSummary("o-dec")).thenReturn(Optional.empty());
        when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                false, null, null, "CARD_DECLINED", "insufficient funds"));
        when(paymentRepository.findById(any())).thenReturn(Optional.of(Payment.initiate("p-dec", "o-dec", "u1", null, Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-decline", fixedInstant)));

        PaymentResult result = service.initiate(new InitiatePaymentCommand(
                "o-dec", "u1", null, new BigDecimal("100"), "VND",
                PaymentGatewayKind.STRIPE, "idem-decline"));

        assertThat(result.status()).isEqualTo("FAILED");
    }

    @Test
    void confirmIdempotencyAlreadyPaid() {
        Payment payment = Payment.initiate("p-paid", "o-paid", "u-paid", null,
                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-paid", fixedInstant);
        payment.markPaid("txn-paid", fixedInstant);
        when(paymentRepository.findById("p-paid")).thenReturn(Optional.of(payment));

        PaymentResult result = service.confirm(new ConfirmPaymentCommand("p-paid", "txn-paid"));

        assertThat(result.status()).isEqualTo("PAID");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void confirmHandlesInvoiceAttachmentFailureGracefully() {
        Payment payment = Payment.initiate("p-fail-inv", "o-fail-inv", "u-fail-inv", null,
                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-fail-inv", fixedInstant);
        when(paymentRepository.findById("p-fail-inv")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceStorage.storeInvoiceUrl(any(), any())).thenThrow(new RuntimeException("Storage unavailable"));

        PaymentResult result = service.confirm(new ConfirmPaymentCommand("p-fail-inv", "txn-123"));

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.invoiceUrl()).isNull();
        verify(integrationEventPublisher).publishPaymentCaptured(any(), any(), any(), any(), any());
    }

    @Test
    void failIdempotencyAlreadyFailed() {
        Payment payment = Payment.initiate("p-f", "o-f", "u-f", null,
                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-f", fixedInstant);
        payment.markFailed("E1", "reason", fixedInstant);
        when(paymentRepository.findById("p-f")).thenReturn(Optional.of(payment));

        PaymentResult result = service.fail(new FailPaymentCommand("p-f", "E1", "reason"));

        assertThat(result.status()).isEqualTo("FAILED");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void listByOrderIdReturnsPayments() {
        Payment payment = Payment.initiate("p-list", "o-list", "u-list", null,
                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-list", fixedInstant);
        when(paymentRepository.findByOrderId("o-list")).thenReturn(java.util.List.of(payment));

        java.util.List<PaymentResult> results = service.listByOrderId("o-list");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).paymentId()).isEqualTo("p-list");
    }
}
