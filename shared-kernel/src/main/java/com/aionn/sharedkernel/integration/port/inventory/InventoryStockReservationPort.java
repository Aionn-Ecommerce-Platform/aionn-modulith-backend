package com.aionn.sharedkernel.integration.port.inventory;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryStockReservationPort {

    List<Reservation> reserveAll(String orderId, List<ReservationLine> lines, int ttlSeconds);

    void commit(String reservationId);

    void release(String reservationId, String reason);

    record ReservationLine(String skuId, String warehouseId, int qty, BigDecimal unitPrice, String currency) {
    }

    record Reservation(String reservationId, String skuId, String warehouseId, int qty, BigDecimal unitPrice,
            String currency) {
    }

    class ReservationException extends RuntimeException {
        private final String skuId;

        public ReservationException(String skuId, String message) {
            super(message);
            this.skuId = skuId;
        }

        public String getSkuId() {
            return skuId;
        }
    }
}
