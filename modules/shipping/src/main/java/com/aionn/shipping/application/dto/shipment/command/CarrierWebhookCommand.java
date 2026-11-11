package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.sharedkernel.application.command.Command;

public record CarrierWebhookCommand(
        String trackingCode,
        String type,
        String currentLocation,
        String statusDesc,
        String shipperName,
        String shipperPhone,
        String signatureUrl,
        String reason,
        String warehouseId,
        String webhookSecret,
        String rawPayload,
        String signature,
        String timestamp,
        String eventId) implements Command {

    public CarrierWebhookCommand(String trackingCode, String type, String currentLocation,
            String statusDesc, String shipperName, String shipperPhone, String signatureUrl,
            String reason, String warehouseId, String webhookSecret) {
        this(trackingCode, type, currentLocation, statusDesc, shipperName, shipperPhone,
                signatureUrl, reason, warehouseId, webhookSecret, null, null, null, null);
    }
}
