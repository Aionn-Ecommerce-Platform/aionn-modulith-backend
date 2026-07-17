package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.AdjustWarehousePriorityInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdjustPriorityUseCase implements AdjustWarehousePriorityInputPort {

    private final WarehouseService warehouseService;

    @Override
    @Transactional
    public WarehouseResult execute(AdjustPriorityCommand command) {
        return warehouseService.adjustPriority(command);
    }
}
