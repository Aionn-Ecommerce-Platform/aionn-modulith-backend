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
}
