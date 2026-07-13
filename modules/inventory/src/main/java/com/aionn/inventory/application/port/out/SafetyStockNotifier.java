package com.aionn.inventory.application.port.out;

public interface SafetyStockNotifier {

    void notifySafetyStockBreach(
            String merchantId,
            String skuId,
            String warehouseId,
            int availableQty,
            int safetyStockQty);
}
