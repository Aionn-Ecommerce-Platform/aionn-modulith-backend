package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.util.List;

public record QuoteShippingCommand(
        String orderId,
        ShipmentAddress address,
        List<Parcel> parcels,
        BigDecimal goodsSubtotal,
        String currency,
        String serviceCode) implements Command {

    /** Backwards-compatible constructor for callers without a goods subtotal. */
    public QuoteShippingCommand(String orderId, ShipmentAddress address, List<Parcel> parcels, String currency) {
        this(orderId, address, parcels, null, currency, "STANDARD");
    }

    public QuoteShippingCommand(String orderId, ShipmentAddress address, List<Parcel> parcels,
            BigDecimal goodsSubtotal, String currency) {
        this(orderId, address, parcels, goodsSubtotal, currency, "STANDARD");
    }

    /**
     * Parcel characteristics for one SKU in the shipment. {@code weightGram} is
     * the total weight for {@code qty} units; box dims describe one unit.
     */
    public record Parcel(
            Integer weightGram,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            int qty) {
    }
}
