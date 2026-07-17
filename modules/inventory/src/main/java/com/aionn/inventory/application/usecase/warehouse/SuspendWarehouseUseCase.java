package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.SuspendWarehouseInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuspendWarehouseUseCase implements SuspendWarehouseInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional
    public WarehouseResult execute(SuspendWarehouseCommand command) {
        return warehouseService.suspend(command);
    }
}
