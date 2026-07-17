package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

    private static final String RES_ID = "RES_1";
    private static final String SKU = "SKU_1";
    private static final String WH = "WH_1";
    private static final String ORDER = "ORDER_1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void reserveCreatesReservationInReservedStatusAndEmitsEvent() {
        Instant expiresAt = FIXED_CLOCK.instant().plus(Duration.ofMinutes(15));

        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt, FIXED_CLOCK);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(r.getReservationId()).isEqualTo(RES_ID);
        assertThat(r.getQty()).isEqualTo(5);
        assertThat(r.getReservedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(r.peekEvents())
                .anyMatch(env -> env.payload() instanceof StockReservationEvents.StockReserved);
    }

    @Test
    void reserveRejectsZeroOrNegativeQty() {
        Instant expiresAt = FIXED_CLOCK.instant().plus(Duration.ofMinutes(15));

        assertThatThrownBy(() -> StockReservation.reserve(RES_ID, SKU, WH, ORDER, 0, expiresAt, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void failedFactoryProducesFailedReservation() {
        StockReservation r = StockReservation.failed(RES_ID, SKU, WH, 5, "Insufficient stock", FIXED_CLOCK);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(r.getOrderId()).isNull();
        assertThat(r.getReservedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(r.peekEvents())
                .anyMatch(env -> env.payload() instanceof StockReservationEvents.StockReservationFailed);
    }

    @Test
    void commitTransitionsFromReservedToCommitted() {
        Instant expiresAt = FIXED_CLOCK.instant().plus(Duration.ofMinutes(15));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt, FIXED_CLOCK);
        r.pullEvents();

        r.commit(FIXED_CLOCK);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
        assertThat(r.getDecidedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(r.peekEvents())
                .anyMatch(env -> env.payload() instanceof StockReservationEvents.StockCommitted);
    }

    @Test
    void releaseTransitionsFromReservedToReleased() {
        Instant expiresAt = FIXED_CLOCK.instant().plus(Duration.ofMinutes(15));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt, FIXED_CLOCK);

        r.release("order cancelled", FIXED_CLOCK);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(r.getDecidedAt()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    void commitRejectsWhenAlreadyCommitted() {
        Instant expiresAt = FIXED_CLOCK.instant().plus(Duration.ofMinutes(15));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt, FIXED_CLOCK);
        r.commit(FIXED_CLOCK);

        assertThatThrownBy(() -> r.commit(FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE.getCode());
    }

    @Test
    void isExpiredReturnsTrueWhenNowAfterExpiresAt() {
        Instant expiresAt = FIXED_CLOCK.instant().minus(Duration.ofMinutes(1));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt, FIXED_CLOCK);

        assertThat(r.isExpired(FIXED_CLOCK.instant())).isTrue();
    }
}
