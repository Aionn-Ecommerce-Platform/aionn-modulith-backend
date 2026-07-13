package com.aionn.inventory.application.mapper;

import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.domain.model.InventoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryItemResultMapper {

    @Mapping(target = "skuId", expression = "java(item.getKey().skuId())")
    @Mapping(target = "warehouseId", expression = "java(item.getKey().warehouseId())")
    @Mapping(target = "reservedQty", expression = "java(item.reservedQty())")
    InventoryItemResult toResult(InventoryItem item);
}
