package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.WarehouseEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarehouseTest {

    private static final String WH_ID = "WH_1";
    private static final String MERCHANT = "M_1";

    @Test
    void createSetsActiveStatusAndEmitsEvent() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);

        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.ACTIVE);
        assertThat(w.getMerchantId()).isEqualTo(MERCHANT);
        assertThat(w.getPriorityLevel()).isEqualTo(1);
        assertThat(w.peekEvents())
                .anyMatch(env -> env.payload() instanceof WarehouseEvents.WarehouseCreated);
    }

    @Test
    void createRejectsBlankMerchant() {
        assertThatThrownBy(() -> Warehouse.create(WH_ID, " ", "addr", 1))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void createRejectsNegativePriority() {
        assertThatThrownBy(() -> Warehouse.create(WH_ID, MERCHANT, "addr", -1))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void ensureOwnedByThrowsWhenMerchantMismatch() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);

        assertThatThrownBy(() -> w.ensureOwnedBy("other-merchant"))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_FORBIDDEN.getCode());
    }

    @Test
    void changeStatusAllowsActiveToInactive() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);
        w.pullEvents();

        w.changeStatus(WarehouseStatus.INACTIVE);

        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.INACTIVE);
        assertThat(w.peekEvents())
                .anyMatch(env -> env.payload() instanceof WarehouseEvents.WarehouseStatusChanged);
    }

    @Test
    void changeStatusRejectsDirectSuspended() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);

        assertThatThrownBy(() -> w.changeStatus(WarehouseStatus.SUSPENDED))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION.getCode());
    }

    @Test
    void suspendThenLiftReturnsToActive() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);
        w.suspend("admin", "fraud-check");
        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.SUSPENDED);

        w.liftSuspension();

        assertThat(w.getStatus()).isEqualTo(WarehouseStatus.ACTIVE);
    }

    @Test
    void adjustPriorityUpdatesValue() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);

        w.adjustPriority(5);

        assertThat(w.getPriorityLevel()).isEqualTo(5);
        assertThat(w.peekEvents())
                .anyMatch(env -> env.payload() instanceof WarehouseEvents.WarehousePriorityAdjusted);
    }

    @Test
    void liftSuspensionRejectsWhenNotSuspended() {
        Warehouse w = Warehouse.create(WH_ID, MERCHANT, "addr", 1);

        assertThatThrownBy(w::liftSuspension)
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION.getCode());
    }
}
