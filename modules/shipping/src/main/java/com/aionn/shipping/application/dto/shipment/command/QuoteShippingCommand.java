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

    public QuoteShippingCommand(String orderId, ShipmentAddress address, List<Parcel> parcels, String currency) {
        this(orderId, address, parcels, null, currency, "STANDARD");
    }

    public QuoteShippingCommand(String orderId, ShipmentAddress address, List<Parcel> parcels,
            BigDecimal goodsSubtotal, String currency) {
        this(orderId, address, parcels, goodsSubtotal, currency, "STANDARD");
    }

    public record Parcel(
            String skuId,
            Integer weightGram,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            int qty) {
    }
}
