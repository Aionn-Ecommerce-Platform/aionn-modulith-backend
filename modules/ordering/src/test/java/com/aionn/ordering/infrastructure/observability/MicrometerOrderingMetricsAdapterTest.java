package com.aionn.ordering.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicrometerOrderingMetricsAdapterTest {

    @Mock
    private MeterRegistry registry;

    @Mock
    private Counter counter;

    private MicrometerOrderingMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MicrometerOrderingMetricsAdapter(registry);
    }

    @Test
    void orderLifecycleIncrementsCounter() {
        when(registry.counter(eq("ordering.order.lifecycle"), eq("transition"), eq("placed")))
                .thenReturn(counter);

        adapter.orderLifecycle("placed");

        verify(counter).increment();
    }

    @Test
    void cartLifecycleIncrementsCounter() {
        when(registry.counter(eq("ordering.cart.lifecycle"), eq("transition"), eq("updated")))
                .thenReturn(counter);

        adapter.cartLifecycle("updated");

        verify(counter).increment();
    }

    @Test
    void returnLifecycleIncrementsCounter() {
        when(registry.counter(eq("ordering.return.lifecycle"), eq("transition"), eq("requested")))
                .thenReturn(counter);

        adapter.returnLifecycle("requested");

        verify(counter).increment();
    }

    @Test
    void placeOrderOutcomeIncrementsCounter() {
        when(registry.counter(eq("ordering.place_order.outcome"), eq("outcome"), eq("success")))
                .thenReturn(counter);

        adapter.placeOrderOutcome("success");

        verify(counter).increment();
    }

    @Test
    void autoCancelledIncrementsCounter() {
        when(registry.counter(eq("ordering.order.auto_cancelled"))).thenReturn(counter);

        adapter.autoCancelled(5);

        verify(counter).increment(5);
    }

    @Test
    void gatewayOutcomeIncrementsCounter() {
        when(registry.counter(eq("ordering.gateway.outcome"), eq("gateway"), eq("payment"), eq("outcome"), eq("success")))
                .thenReturn(counter);

        adapter.gatewayOutcome("payment", "success");

        verify(counter).increment();
    }
}
