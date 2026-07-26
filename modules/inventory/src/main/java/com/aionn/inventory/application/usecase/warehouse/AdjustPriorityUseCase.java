package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.mapper.WarehouseResultMapper;
import com.aionn.inventory.application.port.in.warehouse.AdjustWarehousePriorityInputPort;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdjustPriorityUseCase implements AdjustWarehousePriorityInputPort {

    private final WarehouseService warehouseService;
    private final WarehouseResultMapper warehouseResultMapper;

    @Override
    @Transactional
    public WarehouseResult execute(AdjustPriorityCommand command) {
        return warehouseResultMapper.toResult(warehouseService.adjustPriority(command));
    }
}
