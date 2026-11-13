package com.aionn.shipping.adapter.rest.mapper.shipment;

import com.aionn.shipping.adapter.rest.dto.shipment.request.CarrierWebhookRequest;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import org.springframework.stereotype.Component;

@Component
public class ShippingWebhookDtoMapper {

    public CarrierWebhookCommand toCommand(
            CarrierWebhookRequest request,
            String legacySecret,
            String rawPayload,
            String signature,
            String timestamp,
            String eventId) {
        return new CarrierWebhookCommand(
                request.trackingCode(),
                request.type(),
                request.currentLocation(),
                request.statusDesc(),
                request.shipperName(),
                request.shipperPhone(),
                request.signatureUrl(),
                request.reason(),
                request.warehouseId(),
                legacySecret,
                rawPayload,
                signature,
                timestamp,
                eventId);
    }
}
