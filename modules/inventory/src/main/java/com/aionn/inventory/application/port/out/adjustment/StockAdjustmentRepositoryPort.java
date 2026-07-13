package com.aionn.inventory.application.port.out.adjustment;

import com.aionn.inventory.domain.model.StockAdjustment;

import java.util.List;

public interface StockAdjustmentRepositoryPort {

    StockAdjustment save(StockAdjustment adjustment);

    List<StockAdjustment> findBySkuAndWarehouse(String skuId, String warehouseId, int page, int size);
}
