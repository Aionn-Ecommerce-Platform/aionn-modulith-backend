package com.aionn.inventory.application.usecase.warehouse;

import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.query.ListWarehousesByOwnerQuery;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.*;
import com.aionn.inventory.application.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseUseCase implements
        CreateWarehouseInputPort,
        ChangeWarehouseStatusInputPort,
        AdjustWarehousePriorityInputPort,
        SuspendWarehouseInputPort,
        LiftWarehouseSuspensionInputPort,
        GetWarehouseInputPort,
        ListWarehousesByOwnerInputPort {

    private final WarehouseService service;

    @Override
    public WarehouseResult execute(CreateWarehouseCommand command) {
        return service.create(command);
    }

    @Override
    public WarehouseResult execute(ChangeStatusCommand command) {
        return service.changeStatus(command);
    }

    @Override
    public WarehouseResult execute(AdjustPriorityCommand command) {
        return service.adjustPriority(command);
    }

    @Override
    public WarehouseResult execute(SuspendWarehouseCommand command) {
        return service.suspend(command);
    }

    @Override
    public WarehouseResult execute(LiftSuspensionCommand command) {
        return service.liftSuspension(command);
    }

    @Override
    public WarehouseResult execute(String warehouseId) {
        return service.get(warehouseId);
    }

    @Override
    public List<WarehouseResult> execute(ListWarehousesByOwnerQuery query) {
        return service.listByOwner(query.ownerId());
    }
}
