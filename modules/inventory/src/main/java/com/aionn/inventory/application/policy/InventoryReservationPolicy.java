package com.aionn.inventory.application.policy;

public interface InventoryReservationPolicy {

    int getDefaultTtlSeconds();

    int getAutoReleaseBatchSize();
}
