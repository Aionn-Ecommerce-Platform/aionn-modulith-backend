package com.aionn.inventory.adapter.rest.mapper.warehouse;

import com.aionn.inventory.adapter.rest.dto.warehouse.AdjustPriorityRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.ChangeWarehouseStatusRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.CreateWarehouseRequest;
import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseDtoMapper {

    CreateWarehouseCommand toCreateWarehouseCommand(String ownerId, CreateWarehouseRequest request);

    ChangeStatusCommand toChangeStatusCommand(String warehouseId, String ownerId, ChangeWarehouseStatusRequest request);

    AdjustPriorityCommand toAdjustPriorityCommand(String warehouseId, String ownerId, AdjustPriorityRequest request);

    SuspendWarehouseCommand toSuspendWarehouseCommand(String warehouseId, String adminId, AdminReasonRequest request);
}
