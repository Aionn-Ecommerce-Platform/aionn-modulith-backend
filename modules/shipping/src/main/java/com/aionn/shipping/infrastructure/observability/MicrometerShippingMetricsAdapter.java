package com.aionn.shipping.infrastructure.observability;

import com.aionn.shipping.application.port.out.observability.ShippingMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerShippingMetricsAdapter implements ShippingMetricsPort {

    private final MeterRegistry registry;

    public MicrometerShippingMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void shipmentLifecycle(String transition) {
        registry.counter("shipping.shipment.lifecycle", "transition", transition).increment();
    }

    @Override
    public void rateLifecycle(String transition) {
        registry.counter("shipping.rate.lifecycle", "transition", transition).increment();
    }

    @Override
    public void carrierOutcome(String operation, String outcome) {
        registry.counter("shipping.carrier.outcome",
                "operation", operation, "outcome", outcome).increment();
    }

    @Override
    public void quoteOutcome(String source) {
        registry.counter("shipping.quote.source", "source", source).increment();
    }
}
