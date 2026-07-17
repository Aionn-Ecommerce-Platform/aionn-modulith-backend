package com.aionn.inventory.application.port.out;

import com.aionn.inventory.domain.model.StockAdjustment;

public interface StockAdjustmentPersistencePort {

    StockAdjustment save(StockAdjustment adjustment);
}

