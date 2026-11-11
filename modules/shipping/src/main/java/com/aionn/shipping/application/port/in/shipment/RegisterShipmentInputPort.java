package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface RegisterShipmentInputPort {
    /**
     * HTTP path: ownerId is the authenticated user id; resolved to a merchant.
     */
    ShipmentResult execute(String shipmentId, String ownerId);

    /**
     * Internal path (order placement): the order's merchantId is already known
     * and trusted, so it's verified directly rather than re-resolved.
     */
    ShipmentResult executeByMerchant(String shipmentId, String merchantId);
}
