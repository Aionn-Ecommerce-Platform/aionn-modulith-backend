package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReservationTest {

    private static final String RES_ID = "RES_1";
    private static final String SKU = "SKU_1";
    private static final String WH = "WH_1";
    private static final String ORDER = "ORDER_1";

    @Test
    void reserveCreatesReservationInReservedStatusAndEmitsEvent() {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(r.getReservationId()).isEqualTo(RES_ID);
        assertThat(r.getQty()).isEqualTo(5);
        assertThat(r.peekEvents())
                .anyMatch(env -> env.payload() instanceof StockReservationEvents.StockReserved);
    }

    @Test
    void reserveRejectsZeroOrNegativeQty() {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        assertThatThrownBy(() -> StockReservation.reserve(RES_ID, SKU, WH, ORDER, 0, expiresAt))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void failedFactoryProducesFailedReservation() {
        StockReservation r = StockReservation.failed(RES_ID, SKU, WH, 5, "Insufficient stock");

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(r.getOrderId()).isNull();
        assertThat(r.peekEvents())
                .anyMatch(env -> env.payload() instanceof StockReservationEvents.StockReservationFailed);
    }

    @Test
    void commitTransitionsFromReservedToCommitted() {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt);
        r.pullEvents();

        r.commit();

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
        assertThat(r.getDecidedAt()).isNotNull();
        assertThat(r.peekEvents())
                .anyMatch(env -> env.payload() instanceof StockReservationEvents.StockCommitted);
    }

    @Test
    void releaseTransitionsFromReservedToReleased() {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt);

        r.release("order cancelled");

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void commitRejectsWhenAlreadyCommitted() {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt);
        r.commit();

        assertThatThrownBy(r::commit)
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE.getCode());
    }

    @Test
    void isExpiredReturnsTrueWhenNowAfterExpiresAt() {
        Instant expiresAt = Instant.now().minus(Duration.ofMinutes(1));
        StockReservation r = StockReservation.reserve(RES_ID, SKU, WH, ORDER, 5, expiresAt);

        assertThat(r.isExpired(Instant.now())).isTrue();
    }
}
