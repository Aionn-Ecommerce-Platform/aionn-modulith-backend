package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.GetWarehouseInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWarehouseUseCase implements GetWarehouseInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional(readOnly = true)
    public WarehouseResult execute(String warehouseId) {
        return warehouseService.get(warehouseId);
    }
}
