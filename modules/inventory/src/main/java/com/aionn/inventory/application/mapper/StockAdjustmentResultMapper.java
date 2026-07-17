package com.aionn.inventory.application.mapper;

import com.aionn.inventory.application.dto.inventory.result.StockAdjustmentResult;
import com.aionn.inventory.domain.model.StockAdjustment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockAdjustmentResultMapper {

    @Mapping(target = "type", expression = "java(adjustment.getType().name())")
    StockAdjustmentResult toResult(StockAdjustment adjustment);
}
