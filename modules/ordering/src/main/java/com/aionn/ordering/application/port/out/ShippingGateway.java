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

    /** Quote for a specific service level (STANDARD / EXPRESS). */
    default ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
            List<Parcel> parcels, BigDecimal goodsSubtotal, String serviceCode) {
        return quote(orderId, merchantId, address, currency, parcels, goodsSubtotal);
    }

    /** One registration per carrier consignment (multi-parcel orders). */
    List<Registration> createAndRegister(String orderId, String merchantId, String userId,
            ShippingAddress address, BigDecimal codAmount, BigDecimal shippingFee, String currency,
            List<Parcel> parcels);

    /** True when the carrier has taken at least one parcel of the order. */
    boolean hasPickedUpShipment(String orderId);

    /** True while at least one non-cancelled shipment still exists for the order. */
    boolean hasActiveShipment(String orderId);

    /** True when a non-cancelled shipment has not been registered with the carrier yet. */
    boolean hasUnregisteredShipment(String orderId);

    /** True when every non-cancelled shipment of the order has been delivered. */
    boolean allShipmentsDelivered(String orderId);

    record ShippingQuote(BigDecimal fee, String currency) {
    }

    record Registration(String shipmentId, String trackingCode, String carrierOrderId, String labelUrl) {
    }

    /** weightGram is the total for qty units; box dims describe one unit. */
    record Parcel(Integer weightGram, BigDecimal lengthCm, BigDecimal widthCm,
            BigDecimal heightCm, int qty) {
    }
}
