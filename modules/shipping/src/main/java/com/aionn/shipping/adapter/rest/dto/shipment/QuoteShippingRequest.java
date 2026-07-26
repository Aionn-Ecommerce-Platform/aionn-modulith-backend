package com.aionn.shipping.adapter.rest.dto.shipment;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuoteShippingRequest(
        @NotBlank String orderId,
        @NotNull @Valid ShipmentAddress address,
        @NotNull @Valid ShipmentDimensions dimensions,
        @Size(min = 3, max = 3) String currency) {
}

