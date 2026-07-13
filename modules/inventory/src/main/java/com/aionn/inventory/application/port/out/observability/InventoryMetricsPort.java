package com.aionn.inventory.application.port.out.observability;

public interface InventoryMetricsPort {

    void reservationOutcome(String outcome);

    void inventoryLifecycle(String transition);

    void warehouseLifecycle(String transition);

    void transferLifecycle(String transition);

    void safetyStockBreach();

    void autoReleased(int count);

    void notifierOutcome(String notifier, String outcome);
}
