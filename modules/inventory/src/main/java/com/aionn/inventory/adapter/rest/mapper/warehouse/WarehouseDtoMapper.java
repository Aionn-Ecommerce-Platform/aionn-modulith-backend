package com.aionn.inventory.adapter.rest.mapper.warehouse;

import com.aionn.inventory.adapter.rest.dto.warehouse.request.AdjustPriorityRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.request.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.request.ChangeWarehouseStatusRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.request.CreateWarehouseRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.response.WarehouseResponse;
import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseDtoMapper {

    CreateWarehouseCommand toCreateWarehouseCommand(String ownerId, CreateWarehouseRequest request);

    ChangeStatusCommand toChangeStatusCommand(String warehouseId, String ownerId, ChangeWarehouseStatusRequest request);

    AdjustPriorityCommand toAdjustPriorityCommand(String warehouseId, String ownerId, AdjustPriorityRequest request);

    SuspendWarehouseCommand toSuspendWarehouseCommand(String warehouseId, String adminId, AdminReasonRequest request);

    WarehouseResponse toResponse(WarehouseResult result);

    List<WarehouseResponse> toResponses(List<WarehouseResult> results);
}
