package com.aionn.inventory.application.port.in.warehouse;

import com.aionn.inventory.application.dto.warehouse.query.ListWarehousesByOwnerQuery;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import java.util.List;

public interface ListWarehousesByOwnerInputPort {
    List<WarehouseResult> execute(ListWarehousesByOwnerQuery query);
}
