package com.aionn.ordering.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outbound port to inventory for reserving / committing / releasing stock
 * during the order lifecycle.
 */
public interface StockReservationGateway {

    /**
     * Reserves every line. If any line fails, all previously created reservations
     * are released and a {@link ReservationException} is thrown.
     */
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
