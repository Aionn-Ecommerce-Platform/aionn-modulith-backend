package com.aionn.shipping.application.port.out;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;

import java.math.BigDecimal;
import java.time.Instant;

public interface CarrierClient {

    Quote quote(ShipmentAddress address, ShipmentDimensions dimensions, String currency);

    Registration register(String shipmentId, String orderId, ShipmentAddress address,
            ShipmentDimensions dimensions, BigDecimal codAmount, BigDecimal shippingFee, String currency);

    String fetchLabel(String trackingCode);

    void cancel(String trackingCode, String reason);

    /**
     * Fetch the latest order detail from the carrier (used by the polling
     * worker when no webhook is configured).
     */
    OrderDetail fetchOrderDetail(String trackingCode);

    record Quote(BigDecimal fee, String currency, String zoneCode, String detail,
            Instant expectedDeliveryDate, Instant orderDate) {
    }

    record Registration(String trackingCode, String carrierOrderId, Instant expectedDate) {
    }

    record OrderDetail(
            String status,
            String currentLocation,
            String shipperName,
            String shipperPhone,
            String signatureUrl,
            String reason,
            String warehouseId,
            Instant expectedDeliveryDate) {
    }
}
