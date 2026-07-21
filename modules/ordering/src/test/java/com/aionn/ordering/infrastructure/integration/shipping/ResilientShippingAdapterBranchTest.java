package com.aionn.ordering.infrastructure.integration.shipping;

import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientShippingAdapterBranchTest {

    @Mock
    private ShippingGateway delegate;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private OrderingMetricsPort metrics;

    private ResilientShippingAdapter adapter;

    @BeforeEach
    void setUp() {
        Retry retry = Retry.ofDefaults("ordering-shipping");
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("ordering-shipping");
        when(retryRegistry.retry("ordering-shipping")).thenReturn(retry);
        when(circuitBreakerRegistry.circuitBreaker("ordering-shipping")).thenReturn(circuitBreaker);

        adapter = new ResilientShippingAdapter(List.of(delegate), retryRegistry, circuitBreakerRegistry, metrics);
    }

    @Test
    void createAndRegisterRecordsFailureOnException() {
        doThrow(new RuntimeException("Error")).when(delegate)
                .createAndRegister(anyString(), anyString(), anyString(), any(), any(), any(), anyString());

        assertThrows(RuntimeException.class, () ->
                adapter.createAndRegister("ord-1", "m-1", "usr-1", null, BigDecimal.ZERO, BigDecimal.TEN, "VND"));

        verify(metrics).gatewayOutcome("shipping", "failure");
    }
}
