package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.StockAdjustmentEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockAdjustmentTest {

    @Test
    void manualEmitsRecordedEvent() {
        StockAdjustment adj = StockAdjustment.manual("a-1", "sku", "wh", 5,
                AdjustmentType.MANUAL_INCREASE, "restock");

        assertThat(adj.pullEvents().get(0).payload())
                .isInstanceOf(StockAdjustmentEvents.ManualAdjustmentRecorded.class);
    }

    @Test
    void manualRejectsOutboundType() {
        assertThatThrownBy(() -> StockAdjustment.manual("a-1", "sku", "wh", 5, AdjustmentType.OUTBOUND, "x"))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_ADJUSTMENT_INVALID.getCode());
    }

    @Test
    void outboundRequiresOrderId() {
        assertThatThrownBy(() -> StockAdjustment.outbound("a-1", "sku", "wh", 5, " "))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void outboundEmitsRecordedEvent() {
        StockAdjustment adj = StockAdjustment.outbound("a-1", "sku", "wh", 5, "order-1");

        assertThat(adj.pullEvents().get(0).payload())
                .isInstanceOf(StockAdjustmentEvents.OutboundRecorded.class);
    }
}
