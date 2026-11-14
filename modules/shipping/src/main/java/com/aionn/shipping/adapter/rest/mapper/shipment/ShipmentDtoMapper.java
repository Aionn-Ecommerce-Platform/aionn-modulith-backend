package com.aionn.shipping.adapter.rest.mapper.shipment;

import com.aionn.shipping.adapter.rest.dto.shipment.response.ShipmentResponse;
import com.aionn.shipping.adapter.rest.dto.shipment.request.CancelShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.request.CreateShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.request.QuoteShippingRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.request.ResolveIssueRequest;
import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.command.ResolveIssueCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.service.ParcelResolver;
import com.aionn.shipping.application.usecase.shipment.QuoteShippingUseCase;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShipmentDtoMapper {
    ShipmentResponse toResponse(ShipmentResult result);
    List<ShipmentResponse> toResponses(List<ShipmentResult> results);

    CancelShipmentCommand toCommand(CancelShipmentRequest request, String shipmentId, String ownerId);

    ResolveIssueCommand toCommand(ResolveIssueRequest request, String shipmentId);

    default List<QuoteShippingUseCase.ItemRequest> toQuoteItems(QuoteShippingRequest request) {
        return request.items().stream()
                .map(item -> new QuoteShippingUseCase.ItemRequest(item.skuId(), item.qty()))
                .toList();
    }

    default List<ParcelResolver.Item> toParcelItems(CreateShipmentRequest request) {
        return request.items().stream()
                .map(item -> new ParcelResolver.Item(item.skuId(), item.qty()))
                .toList();
    }

    default CreateShipmentCommand toCreateShipmentCommand(
            String ownerId, CreateShipmentRequest request, List<QuoteShippingCommand.Parcel> parcels) {
        return new CreateShipmentCommand(
                request.orderId(), null, ownerId, request.userId(), request.address(), parcels,
                request.codAmount(), request.shippingFee(), request.currency());
    }

    default FetchLabelCommand toFetchLabelCommand(String shipmentId, String ownerId) {
        return new FetchLabelCommand(shipmentId, ownerId);
    }
}
