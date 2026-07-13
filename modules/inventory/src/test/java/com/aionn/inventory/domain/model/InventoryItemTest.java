package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    private static final InventoryItemKey KEY = new InventoryItemKey("SKU_1", "WH_1");

    @Test
    void initializeSetsAvailableEqualToPhysicalAndEmitsEvent() {
        InventoryItem item = InventoryItem.initialize(KEY, 100);

        assertThat(item.getPhysicalQty()).isEqualTo(100);
        assertThat(item.getAvailableQty()).isEqualTo(100);
        assertThat(item.reservedQty()).isZero();
        assertThat(item.peekEvents())
                .anyMatch(env -> env.payload() instanceof InventoryItemEvents.StockInitialized);
    }

    @Test
    void initializeRejectsNegativeQuantity() {
        assertThatThrownBy(() -> InventoryItem.initialize(KEY, -1))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void reserveDecrementsAvailableButKeepsPhysical() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);

        item.reserve(3);

        assertThat(item.getPhysicalQty()).isEqualTo(10);
        assertThat(item.getAvailableQty()).isEqualTo(7);
        assertThat(item.reservedQty()).isEqualTo(3);
    }

    @Test
    void reserveRejectsWhenAvailableInsufficient() {
        InventoryItem item = InventoryItem.initialize(KEY, 5);

        assertThatThrownBy(() -> item.reserve(6))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVENTORY_INSUFFICIENT_STOCK.getCode());
    }

    @Test
    void commitDecrementsPhysicalLeavingAvailableUnchanged() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);
        item.reserve(4);

        item.commit(4);

        assertThat(item.getPhysicalQty()).isEqualTo(6);
        assertThat(item.getAvailableQty()).isEqualTo(6);
        assertThat(item.reservedQty()).isZero();
    }

    @Test
    void releaseRestoresAvailable() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);
        item.reserve(4);

        item.release(4);

        assertThat(item.getAvailableQty()).isEqualTo(10);
        assertThat(item.reservedQty()).isZero();
    }

    @Test
    void emergencyLockBlocksReservations() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);
        item.emergencyLock("admin", "audit");

        assertThat(item.isLocked()).isTrue();
        assertThatThrownBy(() -> item.reserve(1))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVENTORY_LOCKED.getCode());
    }

    @Test
    void adjustWithManualIncreaseUpdatesBothQuantities() {
        InventoryItem item = InventoryItem.initialize(KEY, 10);

        item.adjust(5, AdjustmentType.MANUAL_INCREASE, "restock");

        assertThat(item.getPhysicalQty()).isEqualTo(15);
        assertThat(item.getAvailableQty()).isEqualTo(15);
    }

    @Test
    void configureSafetyStockEmitsBreachWhenAvailableUnderThreshold() {
        InventoryItem item = InventoryItem.initialize(KEY, 5);
        item.pullEvents();

        item.configureSafetyStock(10);

        assertThat(item.getSafetyStockQty()).isEqualTo(10);
        assertThat(item.peekEvents())
                .anyMatch(env -> env.payload() instanceof InventoryItemEvents.SafetyStockBreached);
    }
}
