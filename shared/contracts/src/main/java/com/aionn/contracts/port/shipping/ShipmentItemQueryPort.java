package com.aionn.contracts.port.shipping;

import java.time.Instant;
import java.util.List;

public interface ShipmentItemQueryPort {

    List<DeliveredItem> deliveredItems(String orderId);

    record DeliveredItem(String skuId, int qty, Instant deliveredAt) {
    }
}
