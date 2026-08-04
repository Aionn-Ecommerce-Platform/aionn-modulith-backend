package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.PaymentInitiation;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
import static org.mockito.Mockito.lenient;
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
        @Mock
        private TransactionTemplate transactionTemplate;

        private final Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
        private final Clock clock = Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);

        private PaymentService service;

        @BeforeEach
        void setUp() {
                lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
                service = new PaymentService(
                                paymentRepository,
                                paymentMethodRepository,
                                ledgerRepository,
                                providerRouter,
                                invoiceStorage,
                                eventPublisher,
                                integrationEventPublisher,
                                orderQueryPort,
                                clock,
                                transactionTemplate);
        }

        private static OrderQueryPort.OrderSummary orderSummary(
                        String orderId, String userId, BigDecimal amount, String currency) {
                return new OrderQueryPort.OrderSummary(orderId, userId, "merchant-1", amount, currency);
        }

        @Test
        void initiateReturnsExistingWhenIdempotencyKeyMatches() {
                Payment existing = Payment.initiate("p1", "o1", "u1", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-1",
                                fixedInstant);
                when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

                PaymentInitiation result = service.initiate(new InitiatePaymentCommand(
                                "o1", "u1", null, PaymentGatewayKind.STRIPE, "idem-1"));

                assertThat(result.payment().getPaymentId()).isEqualTo("p1");
                verify(paymentRepository, never()).save(any());
                verify(providerRouter, never()).route(any());
        }

        @Test
        void initiateAsyncReturnsRedirectUrl() {
                when(paymentRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
                when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
                when(orderQueryPort.findOrderSummary("o2")).thenReturn(Optional.of(
                                orderSummary("o2", "u2", new BigDecimal("100"), "VND")));
                when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                                false, null, "https://vnpay.example/redirect", null, null));

                PaymentInitiation result = service.initiate(new InitiatePaymentCommand(
                                "o2", "u2", null, PaymentGatewayKind.VNPAY, "idem-2"));

                assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.INITIATED);
                assertThat(result.redirectUrl()).isEqualTo("https://vnpay.example/redirect");
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void initiateRejectsUnverifiedMethod() {
                PaymentMethod linked = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok", fixedInstant);
                when(paymentRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
                when(orderQueryPort.findOrderSummary("o3")).thenReturn(Optional.of(
                                orderSummary("o3", "u1", new BigDecimal("50"), "VND")));
                when(paymentMethodRepository.findById("m1")).thenReturn(Optional.of(linked));

                assertThatThrownBy(() -> service.initiate(new InitiatePaymentCommand(
                                "o3", "u1", "m1", PaymentGatewayKind.STRIPE, "idem-3")))
                                .isInstanceOf(PaymentException.class)
                                .extracting("errorCode")
                                .isEqualTo(PaymentErrorCode.METHOD_NOT_VERIFIED.getCode());
        }

        @Test
        void confirmMarksPaidAndPublishesIntegrationEvent() {
                Payment payment = Payment.initiate("p4", "o4", "u4", null,
                                Money.of(new BigDecimal("75"), "VND"), PaymentGatewayKind.STRIPE, "idem-4",
                                fixedInstant);
                when(paymentRepository.findById("p4")).thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
                when(invoiceStorage.storeInvoiceUrl("p4", "o4")).thenReturn("https://invoice.example/p4.pdf");

                Payment result = service.confirm(new ConfirmPaymentCommand(
                                "p4", "txn-4", new BigDecimal("75"), "VND"));

                assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(result.getTransactionNo()).isEqualTo("txn-4");
                assertThat(result.getInvoiceUrl()).isEqualTo("https://invoice.example/p4.pdf");
                verify(ledgerRepository).save(any());
                verify(integrationEventPublisher).publishPaymentCaptured(eq("p4"), eq("o4"), eq("txn-4"),
                                any(BigDecimal.class), eq("VND"));
        }

        @Test
        void failMarksFailedAndPublishesEvent() {
                Payment payment = Payment.initiate("p5", "o5", "u5", null,
                                Money.of(new BigDecimal("75"), "VND"), PaymentGatewayKind.STRIPE, "idem-5",
                                fixedInstant);
                when(paymentRepository.findById("p5")).thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

                Payment result = service.fail(new FailPaymentCommand("p5", "E1", "boom"));

                assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
                verify(integrationEventPublisher).publishPaymentFailed("p5", "o5", "E1", "boom");
        }

        @Test
        void refundCallsProviderAndPersistsLedger() {
                Payment payment = Payment.initiate("p6", "o6", "u6", null,
                                Money.of(new BigDecimal("80"), "VND"), PaymentGatewayKind.STRIPE, "idem-6",
                                fixedInstant);
                payment.markPaid("txn-6", fixedInstant);
                when(paymentRepository.findById("p6")).thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
                when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
                when(providerClient.refund(any())).thenReturn(
                                new PaymentProviderClient.Refund(true, "rf-1", null));

                Payment result = service.refund(new RefundPaymentCommand(
                                "p6", new BigDecimal("80"), "VND", "duplicate"));

                assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
                verify(ledgerRepository).save(any());
                verify(integrationEventPublisher).publishPaymentRefunded(eq("p6"), eq("o6"),
                                eq("rf-1"), any(BigDecimal.class), eq("VND"), eq("duplicate"));
        }

        @Test
        void refundDeclinedFromProviderThrows() {
                Payment payment = Payment.initiate("p7", "o7", "u7", null,
                                Money.of(new BigDecimal("50"), "VND"), PaymentGatewayKind.STRIPE, "idem-7",
                                fixedInstant);
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
                                Money.of(new BigDecimal("50"), "VND"), PaymentGatewayKind.STRIPE, "idem-8",
                                fixedInstant);
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
                when(orderQueryPort.findOrderSummary("o-cap")).thenReturn(Optional
                                .of(new com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort.OrderSummary(
                                                "o-cap", "u1", "m-cap", new BigDecimal("100"), "VND")));
                when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                                true, "txn-cap", null, null, null));
                when(paymentRepository.findById(any())).thenReturn(Optional.of(
                                Payment.initiate("p-cap", "o-cap", "u1", null, Money.of(new BigDecimal("100"), "VND"),
                                                PaymentGatewayKind.STRIPE, "idem-captured", fixedInstant)));

                PaymentInitiation result = service.initiate(new InitiatePaymentCommand(
                                "o-cap", "u1", null, PaymentGatewayKind.STRIPE, "idem-captured"));

                assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        void initiateDeclineCodeDirectly() {
                when(paymentRepository.findByIdempotencyKey("idem-decline")).thenReturn(Optional.empty());
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
                when(providerRouter.route(PaymentGatewayKind.STRIPE)).thenReturn(providerClient);
                when(orderQueryPort.findOrderSummary("o-dec")).thenReturn(Optional.of(
                                orderSummary("o-dec", "u1", new BigDecimal("100"), "VND")));
                when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                                false, null, null, "CARD_DECLINED", "insufficient funds"));
                when(paymentRepository.findById(any())).thenReturn(Optional.of(
                                Payment.initiate("p-dec", "o-dec", "u1", null, Money.of(new BigDecimal("100"), "VND"),
                                                PaymentGatewayKind.STRIPE, "idem-decline", fixedInstant)));

                PaymentInitiation result = service.initiate(new InitiatePaymentCommand(
                                "o-dec", "u1", null, PaymentGatewayKind.STRIPE, "idem-decline"));

                assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        void confirmIdempotencyAlreadyPaid() {
                Payment payment = Payment.initiate("p-paid", "o-paid", "u-paid", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-paid",
                                fixedInstant);
                payment.markPaid("txn-paid", fixedInstant);
                when(paymentRepository.findById("p-paid")).thenReturn(Optional.of(payment));

                Payment result = service.confirm(new ConfirmPaymentCommand(
                                "p-paid", "txn-paid", new BigDecimal("100"), "VND"));

                assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
                verify(paymentRepository, never()).save(any());
        }

        @Test
        void confirmHandlesInvoiceAttachmentFailureGracefully() {
                Payment payment = Payment.initiate("p-fail-inv", "o-fail-inv", "u-fail-inv", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-fail-inv",
                                fixedInstant);
                when(paymentRepository.findById("p-fail-inv")).thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
                when(invoiceStorage.storeInvoiceUrl(any(), any()))
                                .thenThrow(new RuntimeException("Storage unavailable"));

                Payment result = service.confirm(new ConfirmPaymentCommand(
                                "p-fail-inv", "txn-123", new BigDecimal("100"), "VND"));

                assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(result.getInvoiceUrl()).isNull();
                verify(integrationEventPublisher).publishPaymentCaptured(any(), any(), any(), any(), any());
        }

        @Test
        void failIdempotencyAlreadyFailed() {
                Payment payment = Payment.initiate("p-f", "o-f", "u-f", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-f",
                                fixedInstant);
                payment.markFailed("E1", "reason", fixedInstant);
                when(paymentRepository.findById("p-f")).thenReturn(Optional.of(payment));

                Payment result = service.fail(new FailPaymentCommand("p-f", "E1", "reason"));

                assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
                verify(paymentRepository, never()).save(any());
        }

        @Test
        void listByOrderIdReturnsPayments() {
                Payment payment = Payment.initiate("p-list", "o-list", "u-list", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.STRIPE, "idem-list",
                                fixedInstant);
                when(paymentRepository.findByOrderId("o-list")).thenReturn(java.util.List.of(payment));

                java.util.List<Payment> results = service.listByOrderId("o-list");

                assertThat(results).hasSize(1);
                assertThat(results.get(0).getPaymentId()).isEqualTo("p-list");
        }

        @Test
        void initiateUsesAuthoritativeOrderAmountAndCurrency() {
                when(paymentRepository.findByIdempotencyKey("idem-authoritative")).thenReturn(Optional.empty());
                when(orderQueryPort.findOrderSummary("o-authoritative")).thenReturn(Optional.of(
                                orderSummary("o-authoritative", "u1", new BigDecimal("250000"), "VND")));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
                when(providerRouter.route(PaymentGatewayKind.VNPAY)).thenReturn(providerClient);
                when(providerClient.authorize(any())).thenReturn(new PaymentProviderClient.Authorization(
                                false, null, "https://pay.example", null, null));

                PaymentInitiation result = service.initiate(new InitiatePaymentCommand(
                                "o-authoritative", "u1", null,
                                PaymentGatewayKind.VNPAY, "idem-authoritative"));

                assertThat(result.payment().getAmount().amount()).isEqualByComparingTo("250000");
                assertThat(result.payment().getAmount().currency()).isEqualTo("VND");
                ArgumentCaptor<PaymentProviderClient.AuthorizationRequest> request =
                                ArgumentCaptor.forClass(PaymentProviderClient.AuthorizationRequest.class);
                verify(providerClient).authorize(request.capture());
                assertThat(request.getValue().amount()).isEqualByComparingTo("250000");
                assertThat(request.getValue().currency()).isEqualTo("VND");
                assertThat(request.getValue().merchantId()).isEqualTo("merchant-1");
        }

        @Test
        void initiateRejectsOrderOwnedByAnotherUser() {
                when(paymentRepository.findByIdempotencyKey("idem-foreign")).thenReturn(Optional.empty());
                when(orderQueryPort.findOrderSummary("o-foreign")).thenReturn(Optional.of(
                                orderSummary("o-foreign", "other-user", new BigDecimal("100"), "VND")));

                assertThatThrownBy(() -> service.initiate(new InitiatePaymentCommand(
                                "o-foreign", "u1", null, PaymentGatewayKind.STRIPE, "idem-foreign")))
                                .isInstanceOf(PaymentException.class)
                                .extracting("errorCode")
                                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode());

                verify(paymentRepository, never()).save(any());
                verify(providerRouter, never()).route(any());
        }

        @Test
        void confirmRejectsGatewayAmountMismatch() {
                Payment payment = Payment.initiate("p-mismatch", "o1", "u1", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.VNPAY,
                                "idem-mismatch", fixedInstant);
                when(paymentRepository.findById("p-mismatch")).thenReturn(Optional.of(payment));

                assertThatThrownBy(() -> service.confirm(new ConfirmPaymentCommand(
                                "p-mismatch", "txn-1", new BigDecimal("1"), "VND")))
                                .isInstanceOf(PaymentException.class)
                                .extracting("errorCode")
                                .isEqualTo(PaymentErrorCode.INVALID_ARGUMENT.getCode());

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
                verify(paymentRepository, never()).save(any());
                verify(ledgerRepository, never()).save(any());
        }

        @Test
        void confirmRejectsGatewayCurrencyMismatchEvenForPaidReplay() {
                Payment payment = Payment.initiate("p-paid-mismatch", "o1", "u1", null,
                                Money.of(new BigDecimal("100"), "VND"), PaymentGatewayKind.VNPAY,
                                "idem-paid-mismatch", fixedInstant);
                payment.markPaid("txn-original", fixedInstant);
                when(paymentRepository.findById("p-paid-mismatch")).thenReturn(Optional.of(payment));

                assertThatThrownBy(() -> service.confirm(new ConfirmPaymentCommand(
                                "p-paid-mismatch", "txn-replay", new BigDecimal("100"), "USD")))
                                .isInstanceOf(PaymentException.class)
                                .extracting("errorCode")
                                .isEqualTo(PaymentErrorCode.INVALID_ARGUMENT.getCode());

                verify(paymentRepository, never()).save(any());
        }
}
