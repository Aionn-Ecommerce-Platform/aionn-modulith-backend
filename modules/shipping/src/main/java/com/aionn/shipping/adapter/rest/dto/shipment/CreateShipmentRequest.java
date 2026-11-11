package com.aionn.shipping.adapter.rest.dto.shipment;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String userId,
        @NotNull @Valid ShipmentAddress address,
        @NotEmpty @Valid List<Item> items,
        @NotNull BigDecimal codAmount,
        @NotNull BigDecimal shippingFee,
        @Size(min = 3, max = 3) String currency) {

    /** One line to ship; parcel dims are resolved server-side from the catalog. */
    public record Item(@NotBlank String skuId, @Positive int qty) {
    }
}
