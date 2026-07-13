package com.aionn.inventory.application.mapper;

import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.domain.model.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WarehouseResultMapper {

    @Mapping(target = "status", expression = "java(warehouse.getStatus().name())")
    WarehouseResult toResult(Warehouse warehouse);
}
