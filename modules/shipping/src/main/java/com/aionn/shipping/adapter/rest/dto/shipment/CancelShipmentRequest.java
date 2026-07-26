package com.aionn.shipping.adapter.rest.dto.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelShipmentRequest(@NotBlank @Size(max = 500) String reason) {
}

