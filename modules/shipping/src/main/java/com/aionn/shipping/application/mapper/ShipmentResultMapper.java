package com.aionn.shipping.application.mapper;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.domain.model.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentResultMapper {

    @Mapping(target = "status", expression = "java(shipment.getStatus().name())")
    ShipmentResult toResult(Shipment shipment);
}
