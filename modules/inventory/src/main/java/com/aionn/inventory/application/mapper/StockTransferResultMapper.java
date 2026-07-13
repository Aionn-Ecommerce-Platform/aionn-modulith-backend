package com.aionn.inventory.application.mapper;

import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.domain.model.StockTransfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockTransferResultMapper {

    @Mapping(target = "status", expression = "java(transfer.getStatus().name())")
    StockTransferResult toResult(StockTransfer transfer);
}
