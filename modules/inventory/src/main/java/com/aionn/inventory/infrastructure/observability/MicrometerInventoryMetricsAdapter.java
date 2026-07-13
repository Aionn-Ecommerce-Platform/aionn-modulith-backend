package com.aionn.inventory.infrastructure.observability;

import com.aionn.inventory.application.port.out.observability.InventoryMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerInventoryMetricsAdapter implements InventoryMetricsPort {

    private final MeterRegistry registry;

    public MicrometerInventoryMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void reservationOutcome(String outcome) {
        registry.counter("inventory.reservation.outcome", "outcome", outcome).increment();
    }

    @Override
    public void inventoryLifecycle(String transition) {
        registry.counter("inventory.item.lifecycle", "transition", transition).increment();
    }

    @Override
    public void warehouseLifecycle(String transition) {
        registry.counter("inventory.warehouse.lifecycle", "transition", transition).increment();
    }

    @Override
    public void transferLifecycle(String transition) {
        registry.counter("inventory.transfer.lifecycle", "transition", transition).increment();
    }

    @Override
    public void safetyStockBreach() {
        registry.counter("inventory.safety_stock.breach").increment();
    }

    @Override
    public void autoReleased(int count) {
        if (count > 0) {
            registry.counter("inventory.reservation.auto_released").increment(count);
        }
    }

    @Override
    public void notifierOutcome(String notifier, String outcome) {
        registry.counter("inventory.notifier.outcome", "notifier", notifier, "outcome", outcome).increment();
    }
}
