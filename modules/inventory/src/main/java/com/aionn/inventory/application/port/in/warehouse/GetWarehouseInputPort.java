package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;

public interface GetWarehouseInputPort {
    WarehouseResult execute(String warehouseId);
}
