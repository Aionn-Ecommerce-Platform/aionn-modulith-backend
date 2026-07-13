package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.StockTransferStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTransferTest {

    private static final String T = "01HZTRA000000000000000001";
    private static final String M = "01HZMER000000000000000001";
    private static final String FROM = "01HZWHS000000000000000001";
    private static final String TO = "01HZWHS000000000000000002";
    private static final String SKU = "01HZSKU000000000000000001";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void initiateRejectsSameWarehouse() {
        assertThatThrownBy(() -> StockTransfer.initiate(T, M, FROM, FROM, SKU, 10, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_TRANSFER_SAME_WAREHOUSE.getCode());
    }

    @Test
    void initiateRejectsZeroQty() {
        assertThatThrownBy(() -> StockTransfer.initiate(T, M, FROM, TO, SKU, 0, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void completeTransitionsToCompleted() {
        StockTransfer t = StockTransfer.initiate(T, M, FROM, TO, SKU, 10, FIXED_CLOCK);
        t.pullEvents();

        t.complete(10, FIXED_CLOCK);

        assertThat(t.getStatus()).isEqualTo(StockTransferStatus.COMPLETED);
        assertThat(t.getCompletedAt()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    void completeRejectsExcessReceivedQty() {
        StockTransfer t = StockTransfer.initiate(T, M, FROM, TO, SKU, 10, FIXED_CLOCK);

        assertThatThrownBy(() -> t.complete(11, FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void cancelTransitionsToCancelled() {
        StockTransfer t = StockTransfer.initiate(T, M, FROM, TO, SKU, 10, FIXED_CLOCK);
        t.cancel("damaged", FIXED_CLOCK);

        assertThat(t.getStatus()).isEqualTo(StockTransferStatus.CANCELLED);
        assertThat(t.getCancelledAt()).isEqualTo(FIXED_CLOCK.instant());
    }
}
