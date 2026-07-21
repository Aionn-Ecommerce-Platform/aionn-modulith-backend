package com.aionn.payment.infrastructure.observability;

import com.aionn.payment.infrastructure.observability.MicrometerPaymentMetricsAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MicrometerPaymentMetricsAdapterTest {

    private MeterRegistry registry;
    private MicrometerPaymentMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        adapter = new MicrometerPaymentMetricsAdapter(registry);
    }

    @Test
    void shouldIncrementCountersWithoutErrors() {
        assertDoesNotThrow(() -> {
            adapter.paymentLifecycle("AUTHORIZED");
            adapter.methodLifecycle("CREATED");
            adapter.ledgerEntry("CREDIT");
            adapter.providerOutcome("VNPAY", "AUTHORIZE", "SUCCESS");
            adapter.reconciliation("STRIPE", 10, 2);
        });
    }
}
