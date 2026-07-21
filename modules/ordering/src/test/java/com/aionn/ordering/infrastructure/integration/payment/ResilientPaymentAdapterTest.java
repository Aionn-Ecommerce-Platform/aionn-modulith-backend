package com.aionn.ordering.infrastructure.integration.payment;

import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientPaymentAdapterTest {

    @Mock
    private PaymentGateway delegate;

    @Mock
    private OrderingMetricsPort metrics;

    private ResilientPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        adapter = new ResilientPaymentAdapter(List.of(delegate), retryRegistry, circuitBreakerRegistry, metrics);
    }

    @Test
    void authorizeDelegatesAndRecordsMetricsOnSuccess() {
        PaymentGateway.PaymentAuthorization expectedAuth =
                new PaymentGateway.PaymentAuthorization("pay-1", true, null);
        when(delegate.authorize("ord-1", "usr-1", "pm-1", BigDecimal.TEN, "VND", "stripe"))
                .thenReturn(expectedAuth);

        PaymentGateway.PaymentAuthorization result =
                adapter.authorize("ord-1", "usr-1", "pm-1", BigDecimal.TEN, "VND", "stripe");

        assertEquals(expectedAuth, result);
        verify(metrics).gatewayOutcome("payment", "success");
    }

    @Test
    void refundDelegatesAndRecordsMetricsOnSuccess() {
        adapter.refund("pay-1", BigDecimal.TEN, "VND", "return approved");

        verify(delegate).refund("pay-1", BigDecimal.TEN, "VND", "return approved");
        verify(metrics).gatewayOutcome("payment", "success");
    }

    @Test
    void authorizeRecordsFailureMetricOnException() {
        doThrow(new RuntimeException("Gateway error"))
                .when(delegate).authorize("ord-1", "usr-1", "pm-1", BigDecimal.TEN, "VND", "stripe");

        assertThrows(RuntimeException.class, () ->
                adapter.authorize("ord-1", "usr-1", "pm-1", BigDecimal.TEN, "VND", "stripe"));

        verify(metrics).gatewayOutcome("payment", "failure");
    }
}
