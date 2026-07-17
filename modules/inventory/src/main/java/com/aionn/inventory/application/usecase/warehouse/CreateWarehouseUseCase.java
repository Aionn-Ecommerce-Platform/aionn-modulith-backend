package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.CreateWarehouseInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateWarehouseUseCase implements CreateWarehouseInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional
    public WarehouseResult execute(CreateWarehouseCommand command) {
        return warehouseService.create(command);
    }
}
