package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.observability.PaymentMetricsPort;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProviderRoutersTest {

    @Mock
    private PaymentProviderClient stripeClient;
    @Mock
    private PaymentMetricsPort metrics;

    private DefaultPaymentProviderRouter defaultRouter;
    private ResilientPaymentProviderRouter resilientRouter;

    @BeforeEach
    void setUp() {
        when(stripeClient.kind()).thenReturn(PaymentGatewayKind.STRIPE);
        defaultRouter = new DefaultPaymentProviderRouter(List.of(stripeClient));
        defaultRouter.buildIndex();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();

        resilientRouter = new ResilientPaymentProviderRouter(defaultRouter, retryRegistry, cbRegistry, metrics);
    }

    @Test
    void defaultRouterShouldRouteToConfiguredClient() {
        PaymentProviderClient client = defaultRouter.route(PaymentGatewayKind.STRIPE);
        assertNotNull(client);
        assertEquals(PaymentGatewayKind.STRIPE, client.kind());

        assertThrows(PaymentException.class, () -> defaultRouter.route(PaymentGatewayKind.VNPAY));
    }

    @Test
    void resilientRouterShouldWrapDefaultClient() {
        PaymentProviderClient client = resilientRouter.route(PaymentGatewayKind.STRIPE);
        assertNotNull(client);
        assertTrue(client instanceof ResilientPaymentProviderClient);
    }

    @Test
    void resilientClientShouldDelegateKind() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        ResilientPaymentProviderClient resilient = new ResilientPaymentProviderClient(
                stripeClient, retryRegistry, cbRegistry, metrics);

        assertEquals(PaymentGatewayKind.STRIPE, resilient.kind());
    }

    @Test
    void resilientClientShouldDelegateAuthorize() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        ResilientPaymentProviderClient resilient = new ResilientPaymentProviderClient(
                stripeClient, retryRegistry, cbRegistry, metrics);

        PaymentProviderClient.AuthorizationRequest req = new PaymentProviderClient.AuthorizationRequest(
                "pay-1", "order-1", "user-1", null, null,
                java.math.BigDecimal.TEN, "USD", "key-1", null);
        PaymentProviderClient.Authorization expected = new PaymentProviderClient.Authorization(
                true, "pi_123", null, null, null);
        when(stripeClient.authorize(req)).thenReturn(expected);

        PaymentProviderClient.Authorization result = resilient.authorize(req);

        assertEquals(expected, result);
        verify(metrics).providerOutcome("stripe", "authorize", "success");
    }

    @Test
    void resilientClientShouldRecordFailureMetricOnAuthorizeException() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        ResilientPaymentProviderClient resilient = new ResilientPaymentProviderClient(
                stripeClient, retryRegistry, cbRegistry, metrics);

        PaymentProviderClient.AuthorizationRequest req = new PaymentProviderClient.AuthorizationRequest(
                "pay-1", "order-1", "user-1", null, null,
                java.math.BigDecimal.TEN, "USD", "key-1", null);
        when(stripeClient.authorize(req)).thenThrow(new RuntimeException("network error"));

        assertThrows(RuntimeException.class, () -> resilient.authorize(req));
        verify(metrics, atLeastOnce()).providerOutcome("stripe", "authorize", "failure");
    }

    @Test
    void resilientClientShouldDelegateRefund() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        ResilientPaymentProviderClient resilient = new ResilientPaymentProviderClient(
                stripeClient, retryRegistry, cbRegistry, metrics);

        PaymentProviderClient.RefundRequest req = new PaymentProviderClient.RefundRequest(
                "pay-1", "pi_123", java.math.BigDecimal.TEN, "USD", "test reason");
        PaymentProviderClient.Refund expected = new PaymentProviderClient.Refund(true, "re_1", null);
        when(stripeClient.refund(req)).thenReturn(expected);

        PaymentProviderClient.Refund result = resilient.refund(req);

        assertEquals(expected, result);
        verify(metrics).providerOutcome("stripe", "refund", "success");
    }

    @Test
    void resilientClientShouldDelegateVerifyAndParse() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        ResilientPaymentProviderClient resilient = new ResilientPaymentProviderClient(
                stripeClient, retryRegistry, cbRegistry, metrics);

        PaymentProviderClient.WebhookEvent expected = new PaymentProviderClient.WebhookEvent(
                "stripe.payment_intent.succeeded", "pay-1", "pi_1",
                java.math.BigDecimal.TEN, "USD", true, null, null);
        when(stripeClient.verifyAndParse("body", "sig")).thenReturn(expected);

        PaymentProviderClient.WebhookEvent result = resilient.verifyAndParse("body", "sig");

        assertEquals(expected, result);
        verify(metrics).providerOutcome("stripe", "verifyWebhook", "success");
    }

    @Test
    void resilientClientShouldDelegateGenerateInvoice() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        ResilientPaymentProviderClient resilient = new ResilientPaymentProviderClient(
                stripeClient, retryRegistry, cbRegistry, metrics);

        when(stripeClient.generateInvoice("pay-1", "order-1", java.math.BigDecimal.TEN, "USD"))
                .thenReturn("https://invoice.url");

        String result = resilient.generateInvoice("pay-1", "order-1", java.math.BigDecimal.TEN, "USD");

        assertEquals("https://invoice.url", result);
        verify(metrics).providerOutcome("stripe", "generateInvoice", "success");
    }
}
