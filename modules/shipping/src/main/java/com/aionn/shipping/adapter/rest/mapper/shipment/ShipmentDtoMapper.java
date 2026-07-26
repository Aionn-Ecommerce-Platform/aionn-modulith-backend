package com.aionn.shipping.adapter.rest.mapper.shipment;

import com.aionn.shipping.adapter.rest.dto.shipment.response.ShipmentResponse;
import com.aionn.shipping.adapter.rest.dto.shipment.CreateShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.CancelShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.ResolveIssueRequest;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShipmentDtoMapper {
    ShipmentResponse toResponse(ShipmentResult result);
    List<ShipmentResponse> toResponses(List<ShipmentResult> results);

    @Mapping(target = "merchantId", ignore = true)
    CreateShipmentCommand toCommand(CreateShipmentRequest request, String ownerId);

    CancelShipmentCommand toCommand(CancelShipmentRequest request, String shipmentId, String ownerId);

    ResolveIssueCommand toCommand(ResolveIssueRequest request, String shipmentId);

    @Mapping(target = "webhookSecret", source = "secret")
    CarrierWebhookCommand toCommand(com.aionn.shipping.adapter.rest.dto.shipment.CarrierWebhookRequest request, String secret);
}
