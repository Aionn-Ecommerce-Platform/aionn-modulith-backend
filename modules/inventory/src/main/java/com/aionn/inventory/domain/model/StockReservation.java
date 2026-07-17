package com.aionn.inventory.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.ReservationStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class StockReservation extends AggregateRoot {

    private final String reservationId;
    private final String skuId;
    private final String warehouseId;
    private final String orderId;
    private final int qty;
    private ReservationStatus status;
    private final Instant reservedAt;
    private final Instant expiresAt;
    private Instant decidedAt;

    public StockReservation(
            String reservationId,
            String skuId,
            String warehouseId,
            String orderId,
            int qty,
            ReservationStatus status,
            Instant reservedAt,
            Instant expiresAt,
            Instant decidedAt) {
        this.reservationId = reservationId;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.orderId = orderId;
        this.qty = qty;
        this.status = status;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.decidedAt = decidedAt;
    }

    public static StockReservation reserve(
            String reservationId,
            String skuId,
            String warehouseId,
            String orderId,
            int qty,
            Instant expiresAt) {
        return reserve(reservationId, skuId, warehouseId, orderId, qty, expiresAt, Clock.systemUTC());
    }

    public static StockReservation reserve(
            String reservationId,
            String skuId,
            String warehouseId,
            String orderId,
            int qty,
            Instant expiresAt,
            Clock clock) {
        Guard.require(qty > 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "qty must be > 0"));
        Instant now = clock.instant();
        StockReservation reservation = new StockReservation(reservationId, skuId, warehouseId, orderId, qty,
                ReservationStatus.RESERVED, now, expiresAt, null);
        reservation.registerEvent(new StockReservationEvents.StockReserved(
                reservationId, skuId, warehouseId, qty, expiresAt, now));
        return reservation;
    }

    public static StockReservation failed(
            String reservationId, String skuId, String warehouseId, int qty, String reason) {
        return failed(reservationId, skuId, warehouseId, qty, reason, Clock.systemUTC());
    }

    public static StockReservation failed(
            String reservationId, String skuId, String warehouseId, int qty, String reason, Clock clock) {
        Instant now = clock.instant();
        StockReservation reservation = new StockReservation(reservationId, skuId, warehouseId, null, qty,
                ReservationStatus.FAILED, now, null, now);
        reservation.registerEvent(new StockReservationEvents.StockReservationFailed(
                reservationId, skuId, warehouseId, qty, reason, now));
        return reservation;
    }

    public void commit() {
        commit(Clock.systemUTC());
    }

    public void commit(Clock clock) {
        Guard.require(status.canTransitionTo(ReservationStatus.COMMITTED),
                () -> new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE,
                        "Reservation is not in RESERVED state"));
        Instant now = clock.instant();
        this.status = ReservationStatus.COMMITTED;
        this.decidedAt = now;
        registerEvent(new StockReservationEvents.StockCommitted(reservationId, skuId, warehouseId, orderId, qty, now));
    }

    public void release(String reason) {
        release(reason, Clock.systemUTC());
    }

    public void release(String reason, Clock clock) {
        Guard.require(status.canTransitionTo(ReservationStatus.RELEASED),
                () -> new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE,
                        "Reservation is not in RESERVED state"));
        Instant now = clock.instant();
        this.status = ReservationStatus.RELEASED;
        this.decidedAt = now;
        registerEvent(new StockReservationEvents.StockReleased(reservationId, skuId, warehouseId, orderId, qty, reason,
                now));
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    @Override
    protected String aggregateId() {
        return reservationId;
    }
}
