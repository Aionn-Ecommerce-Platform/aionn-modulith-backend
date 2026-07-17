package com.aionn.inventory.infrastructure.policy;

import com.aionn.inventory.application.policy.InventoryReservationPolicy;
import com.aionn.inventory.infrastructure.config.properties.InventoryReservationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringInventoryReservationPolicy implements InventoryReservationPolicy {

    private final InventoryReservationProperties properties;

    @Override
    public int getDefaultTtlSeconds() {
        return properties.defaultTtlSeconds();
    }

    @Override
    public int getAutoReleaseBatchSize() {
        return properties.autoReleaseBatchSize();
    }
}
