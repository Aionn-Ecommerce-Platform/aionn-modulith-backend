package com.aionn.inventory.domain.model;

import com.aionn.inventory.domain.event.StockAdjustmentEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockAdjustmentTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void manualEmitsRecordedEvent() {
        StockAdjustment adj = StockAdjustment.manual("a-1", "sku", "wh", 5,
                AdjustmentType.MANUAL_INCREASE, "restock", FIXED_CLOCK);

        assertThat(adj.pullEvents().get(0).payload())
                .isInstanceOf(StockAdjustmentEvents.ManualAdjustmentRecorded.class);
        assertThat(adj.getOccurredAt()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    void manualRejectsOutboundType() {
        assertThatThrownBy(() -> StockAdjustment.manual("a-1", "sku", "wh", 5, AdjustmentType.OUTBOUND, "x", FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.STOCK_ADJUSTMENT_INVALID.getCode());
    }

    @Test
    void outboundRequiresOrderId() {
        assertThatThrownBy(() -> StockAdjustment.outbound("a-1", "sku", "wh", 5, " ", FIXED_CLOCK))
                .isInstanceOf(InventoryException.class)
                .extracting("errorCode")
                .isEqualTo(InventoryErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void outboundEmitsRecordedEvent() {
        StockAdjustment adj = StockAdjustment.outbound("a-1", "sku", "wh", 5, "order-1", FIXED_CLOCK);

        assertThat(adj.pullEvents().get(0).payload())
                .isInstanceOf(StockAdjustmentEvents.OutboundRecorded.class);
        assertThat(adj.getOccurredAt()).isEqualTo(FIXED_CLOCK.instant());
    }
}
