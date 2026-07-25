package com.aionn.shipping.adapter.rest.dto.shipment;

import jakarta.validation.constraints.NotBlank;

public record CarrierWebhookRequest(
                @NotBlank String trackingCode,
                @NotBlank String type,
                String currentLocation,
                String statusDesc,
                String shipperName,
                String shipperPhone,
                String signatureUrl,
                String reason,
                String warehouseId) {
}
