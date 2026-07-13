package com.aionn.inventory.application.mapper;

import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.domain.model.StockTransfer;
import com.aionn.inventory.domain.model.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryResultMapper {

    WarehouseResult toResult(Warehouse warehouse);

    @Mapping(target = "skuId", source = "key.skuId")
    @Mapping(target = "warehouseId", source = "key.warehouseId")
    @Mapping(target = "reservedQty", expression = "java(item.reservedQty())")
    InventoryItemResult toResult(InventoryItem item);

    StockTransferResult toResult(StockTransfer transfer);

    ReservationResult toResult(StockReservation reservation);
}
