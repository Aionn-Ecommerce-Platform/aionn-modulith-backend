package com.aionn.shipping.adapter.rest.dto.shipment;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String userId,
        @NotNull @Valid ShipmentAddress address,
        @NotNull @Valid ShipmentDimensions dimensions,
        @NotNull BigDecimal codAmount,
        @NotNull BigDecimal shippingFee,
        @Size(min = 3, max = 3) String currency) {
}
