package com.aionn.payment.infrastructure.observability;

import com.aionn.payment.application.port.out.observability.PaymentMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerPaymentMetricsAdapter implements PaymentMetricsPort {

    private final MeterRegistry registry;

    public MicrometerPaymentMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void paymentLifecycle(String transition) {
        registry.counter("payment.lifecycle", "transition", transition).increment();
    }

    @Override
    public void methodLifecycle(String transition) {
        registry.counter("payment.method.lifecycle", "transition", transition).increment();
    }

    @Override
    public void ledgerEntry(String type) {
        registry.counter("payment.ledger.entry", "type", type).increment();
    }

    @Override
    public void providerOutcome(String gateway, String operation, String outcome) {
        registry.counter("payment.provider.outcome",
                "gateway", gateway, "operation", operation, "outcome", outcome).increment();
    }

    @Override
    public void reconciliation(String gateway, int matched, int mismatched) {
        registry.counter("payment.reconciliation.matched", "gateway", gateway).increment(matched);
        registry.counter("payment.reconciliation.mismatched", "gateway", gateway).increment(mismatched);
    }
}
