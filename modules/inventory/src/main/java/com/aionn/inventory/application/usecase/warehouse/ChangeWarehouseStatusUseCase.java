package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.ChangeWarehouseStatusInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeWarehouseStatusUseCase implements ChangeWarehouseStatusInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional
    public WarehouseResult execute(ChangeStatusCommand command) {
        return warehouseService.changeStatus(command);
    }
}
