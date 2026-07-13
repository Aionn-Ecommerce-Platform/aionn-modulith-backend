package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.StockTransferStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTransferTest {

    private static final String T = "01HZTRA000000000000000001";
    private static final String M = "01HZMER000000000000000001";
    private static final String FROM = "01HZWHS000000000000000001";
    private static final String TO = "01HZWHS000000000000000002";
    private static final String SKU = "01HZSKU000000000000000001";

    @Test
    void initiateRejectsSameWarehouse() {
        assertThatThrownBy(() -> StockTransfer.initiate(T, M, FROM, FROM, SKU, 10))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_TRANSFER_SAME_WAREHOUSE.getCode());
    }

    @Test
    void initiateRejectsZeroQty() {
        assertThatThrownBy(() -> StockTransfer.initiate(T, M, FROM, TO, SKU, 0))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void completeTransitionsToCompleted() {
        StockTransfer t = StockTransfer.initiate(T, M, FROM, TO, SKU, 10);
        t.pullEvents();

        t.complete(10);

        assertThat(t.getStatus()).isEqualTo(StockTransferStatus.COMPLETED);
    }

    @Test
    void completeRejectsExcessReceivedQty() {
        StockTransfer t = StockTransfer.initiate(T, M, FROM, TO, SKU, 10);

        assertThatThrownBy(() -> t.complete(11))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void cancelTransitionsToCancelled() {
        StockTransfer t = StockTransfer.initiate(T, M, FROM, TO, SKU, 10);
        t.cancel("damaged");

        assertThat(t.getStatus()).isEqualTo(StockTransferStatus.CANCELLED);
    }
}
