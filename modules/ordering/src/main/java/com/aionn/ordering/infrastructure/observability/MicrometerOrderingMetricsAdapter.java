package com.aionn.ordering.infrastructure.observability;

import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerOrderingMetricsAdapter implements OrderingMetricsPort {

    private final MeterRegistry registry;

    public MicrometerOrderingMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void orderLifecycle(String transition) {
        registry.counter("ordering.order.lifecycle", "transition", transition).increment();
    }

    @Override
    public void cartLifecycle(String transition) {
        registry.counter("ordering.cart.lifecycle", "transition", transition).increment();
    }

    @Override
    public void returnLifecycle(String transition) {
        registry.counter("ordering.return.lifecycle", "transition", transition).increment();
    }

    @Override
    public void placeOrderOutcome(String outcome) {
        registry.counter("ordering.place_order.outcome", "outcome", outcome).increment();
    }

    @Override
    public void autoCancelled(int count) {
        if (count > 0) {
            registry.counter("ordering.order.auto_cancelled").increment(count);
        }
    }

    @Override
    public void gatewayOutcome(String gateway, String outcome) {
        registry.counter("ordering.gateway.outcome", "gateway", gateway, "outcome", outcome).increment();
    }
}
