package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.WarehouseEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarehouseTest {

    private static final String WH_ID = "WH_1";
    private static final String MERCHANT = "M_1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void createSetsActiveStatusAndEmitsEvent() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);

        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.ACTIVE);
        assertThat(w.getMerchantId()).isEqualTo(MERCHANT);
        assertThat(w.getPriorityLevel()).isEqualTo(1);
        assertThat(w.getCreatedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(w.peekEvents())
                .anyMatch(env -> env.payload() instanceof WarehouseEvents.WarehouseCreated);
    }

    @Test
    void createRejectsBlankMerchant() {
        assertThatThrownBy(() -> Warehouse.create(WH_ID, " ", "addr", 1, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void createRejectsNegativePriority() {
        assertThatThrownBy(() -> Warehouse.create(WH_ID, MERCHANT, "addr", -1, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void ensureOwnedByThrowsWhenMerchantMismatch() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);

        assertThatThrownBy(() -> w.ensureOwnedBy("other-merchant"))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode());
    }

    @Test
    void changeStatusAllowsActiveToInactive() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);
        w.pullEvents();

        w.changeStatus(WarehouseStatus.INACTIVE, FIXED_CLOCK);

        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.INACTIVE);
        assertThat(w.getUpdatedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(w.peekEvents())
                .anyMatch(env -> env.payload() instanceof WarehouseEvents.WarehouseStatusChanged);
    }

    @Test
    void changeStatusRejectsDirectSuspended() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);

        assertThatThrownBy(() -> w.changeStatus(WarehouseStatus.SUSPENDED, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION.getCode());
    }

    @Test
    void suspendThenLiftReturnsToActive() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);
        w.suspend("admin", "fraud-check", FIXED_CLOCK);
        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.SUSPENDED);
        assertThat(w.getUpdatedAt()).isEqualTo(FIXED_CLOCK.instant());

        w.liftSuspension(FIXED_CLOCK);

        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.ACTIVE);
        assertThat(w.getUpdatedAt()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    void adjustPriorityUpdatesValue() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);

        w.adjustPriority(5, FIXED_CLOCK);

        assertThat(w.getPriorityLevel()).isEqualTo(5);
        assertThat(w.getUpdatedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(w.peekEvents())
                .anyMatch(env -> env.payload() instanceof WarehouseEvents.WarehousePriorityAdjusted);
    }

    @Test
    void liftSuspensionRejectsWhenNotSuspended() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1, FIXED_CLOCK);

        assertThatThrownBy(() -> w.liftSuspension(FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION.getCode());
    }
}
