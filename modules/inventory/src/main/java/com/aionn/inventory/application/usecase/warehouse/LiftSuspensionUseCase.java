package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.LiftWarehouseSuspensionInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiftSuspensionUseCase implements LiftWarehouseSuspensionInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional
    public WarehouseResult execute(LiftSuspensionCommand command) {
        return warehouseService.liftSuspension(command);
    }
}
