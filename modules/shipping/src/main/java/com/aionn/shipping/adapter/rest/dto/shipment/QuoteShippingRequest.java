package com.aionn.shipping.adapter.rest.dto.shipment;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuoteShippingRequest(
        String orderId,
        @NotNull @Valid ShipmentAddress address,
        @NotEmpty @Valid List<Item> items,
        /** Goods subtotal (before shipping/voucher) for freeship-threshold checks. */
        @PositiveOrZero java.math.BigDecimal goodsSubtotal,
        @Size(min = 3, max = 3) String currency) {

    /**
     * One line to ship. The server resolves parcel characteristics from the
     * catalog by skuId — clients do not send weights/dimensions.
     */
    public record Item(@NotNull String skuId, @Positive int qty) {
    }
}
