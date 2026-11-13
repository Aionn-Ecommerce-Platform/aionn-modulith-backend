package com.aionn.inventory.infrastructure.policy;

import com.aionn.inventory.application.policy.InventoryReservationPolicy;
import com.aionn.inventory.infrastructure.config.InventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringInventoryReservationPolicy implements InventoryReservationPolicy {

    private final InventoryProperties properties;

    @Override
    public int defaultTtlSeconds() {
        return properties.reservation().defaultTtlSeconds();
    }
}
