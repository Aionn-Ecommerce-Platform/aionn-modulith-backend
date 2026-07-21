package com.aionn.ordering.infrastructure.integration.shipping;

import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientShippingAdapterTest {

    @Mock
    private ShippingGateway delegate;

    @Mock
    private OrderingMetricsPort metrics;

    private ResilientShippingAdapter adapter;

    private static ShippingAddress address() {
        return new ShippingAddress("a-1", "John", "+84912345678", "12 Main St", "WARD", "DIST", "PROV", "VN");
    }

    @BeforeEach
    void setUp() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        adapter = new ResilientShippingAdapter(List.of(delegate), retryRegistry, circuitBreakerRegistry, metrics);
    }

    @Test
    void quoteDelegatesAndRecordsMetricsOnSuccess() {
        ShippingGateway.ShippingQuote quote = new ShippingGateway.ShippingQuote(BigDecimal.TEN, "VND");
        when(delegate.quote("ord-1", "m-1", address(), "VND")).thenReturn(quote);

        ShippingGateway.ShippingQuote result = adapter.quote("ord-1", "m-1", address(), "VND");

        assertEquals(quote, result);
        verify(metrics).gatewayOutcome("shipping", "success");
    }

    @Test
    void createAndRegisterDelegatesAndRecordsMetricsOnSuccess() {
        ShippingGateway.Registration registration = new ShippingGateway.Registration("ship-100", "TRACK-1", "CARRIER-1", "http://label");
        when(delegate.createAndRegister("ord-1", "m-1", "usr-1", address(), BigDecimal.ZERO, BigDecimal.TEN, "VND"))
                .thenReturn(registration);

        ShippingGateway.Registration result = adapter.createAndRegister("ord-1", "m-1", "usr-1", address(), BigDecimal.ZERO, BigDecimal.TEN, "VND");

        assertEquals(registration, result);
        verify(metrics).gatewayOutcome("shipping", "success");
    }
}
