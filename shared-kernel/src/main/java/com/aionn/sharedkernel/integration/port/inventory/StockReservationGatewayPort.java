package com.aionn.sharedkernel.integration.port.inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outbound port for reserving and releasing stock during the checkout saga.
 *
 * <p>
 * Used synchronously by the Ordering module so an order can be rejected
 * immediately if any line cannot be reserved.
 * </p>
 */
public interface StockReservationGatewayPort {

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
