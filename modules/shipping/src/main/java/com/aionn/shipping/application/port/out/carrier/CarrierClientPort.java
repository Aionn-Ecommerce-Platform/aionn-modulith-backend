package com.aionn.shipping.application.port.out.carrier;

import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;

import java.math.BigDecimal;
import java.time.Instant;

public interface CarrierClientPort {

    Quote quote(ShipmentAddress address, ShipmentDimensions dimensions, String currency);

    Registration register(String shipmentId, String orderId, ShipmentAddress address,
            ShipmentDimensions dimensions, BigDecimal codAmount, BigDecimal shippingFee, String currency);

    String fetchLabel(String trackingCode);

    void cancel(String trackingCode, String reason);

    record Quote(BigDecimal fee, String currency, String zoneCode, String detail) {
    }

    record Registration(String trackingCode, String carrierOrderId, Instant expectedDate) {
    }
}
