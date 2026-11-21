package com.aionn.shipping.application.port.out;

import java.util.List;

public interface ShipmentItemPersistencePort {

    void saveAll(String shipmentId, String orderId, List<Line> lines);

    record Line(String skuId, int qty) {
    }
}
