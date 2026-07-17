package com.aionn.inventory.application.port.out;

public interface OutboundOrderNotifier {

    void notifyOutbound(String orderId, String reservationId, String skuId, String warehouseId, int qty);

    void notifyReservationFailed(String orderId, String skuId, String warehouseId, int qty, String reason);
}
