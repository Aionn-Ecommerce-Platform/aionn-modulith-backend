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
        String webhookSecret) implements Command {
}
