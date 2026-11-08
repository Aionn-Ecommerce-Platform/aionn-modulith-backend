package com.aionn.ordering.application.port.out;

import com.aionn.ordering.domain.valueobject.ShippingAddress;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outbound port for shipping quote / dispatch when the order is placed or
 * shipping info is changed.
 */
public interface ShippingGateway {

    ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
            List<Parcel> parcels, BigDecimal goodsSubtotal);

    /** One registration per carrier consignment (multi-parcel orders). */
    List<Registration> createAndRegister(String orderId, String merchantId, String userId,
            ShippingAddress address, BigDecimal codAmount, BigDecimal shippingFee, String currency,
            List<Parcel> parcels);

    record ShippingQuote(BigDecimal fee, String currency) {
    }

    record Registration(String shipmentId, String trackingCode, String carrierOrderId, String labelUrl) {
    }

    /**
     * Parcel characteristics for one SKU in the shipment. {@code weightGram} is
     * the total weight for {@code qty} units. Dimensions describe one unit.
     */
    record Parcel(Integer weightGram, java.math.BigDecimal lengthCm, java.math.BigDecimal widthCm,
            java.math.BigDecimal heightCm, int qty) {
    }
}
