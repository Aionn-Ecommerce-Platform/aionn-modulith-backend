package com.aionn.inventory.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.inventory.domain.event.StockAdjustmentEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class StockAdjustment extends AggregateRoot {

        private final String adjId;
        private final String skuId;
        private final String warehouseId;
        private final int qty;
        private final AdjustmentType type;
        private final String reason;
        private final String orderId;
        private final Instant occurredAt;

        public StockAdjustment(
                        String adjId,
                        String skuId,
                        String warehouseId,
                        int qty,
                        AdjustmentType type,
                        String reason,
                        String orderId,
                        Instant occurredAt) {
                this.adjId = adjId;
                this.skuId = skuId;
                this.warehouseId = warehouseId;
                this.qty = qty;
                this.type = type;
                this.reason = reason;
                this.orderId = orderId;
                this.occurredAt = occurredAt;
        }

        public static StockAdjustment manual(
                        String adjId, String skuId, String warehouseId, int qty, AdjustmentType type, String reason) {
                return manual(adjId, skuId, warehouseId, qty, type, reason, Clock.systemUTC());
        }

        public static StockAdjustment manual(
                        String adjId, String skuId, String warehouseId, int qty, AdjustmentType type, String reason, Clock clock) {
                Guard.require(type != AdjustmentType.OUTBOUND,
                                () -> new InventoryException(InventoryErrorCode.STOCK_ADJUSTMENT_INVALID,
                                                "Use outbound(...) to record OUTBOUND adjustments"));
                Instant now = clock.instant();
                StockAdjustment adj = new StockAdjustment(adjId, skuId, warehouseId, qty, type, reason, null, now);
                adj.registerEvent(new StockAdjustmentEvents.ManualAdjustmentRecorded(
                                adjId, skuId, warehouseId, qty, type, reason, now));
                return adj;
        }

        public static StockAdjustment outbound(
                        String adjId, String skuId, String warehouseId, int qty, String orderId) {
                return outbound(adjId, skuId, warehouseId, qty, orderId, Clock.systemUTC());
        }

        public static StockAdjustment outbound(
                        String adjId, String skuId, String warehouseId, int qty, String orderId, Clock clock) {
                Guard.require(orderId != null && !orderId.isBlank(),
                                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "orderId must not be blank"));
                Instant now = clock.instant();
                StockAdjustment adj = new StockAdjustment(adjId, skuId, warehouseId, qty, AdjustmentType.OUTBOUND,
                                "OUTBOUND for order " + orderId, orderId, now);
                adj.registerEvent(new StockAdjustmentEvents.OutboundRecorded(
                                adjId, skuId, warehouseId, qty, orderId, now));
                return adj;
        }

        @Override
        protected String aggregateId() {
                return adjId;
        }
}
