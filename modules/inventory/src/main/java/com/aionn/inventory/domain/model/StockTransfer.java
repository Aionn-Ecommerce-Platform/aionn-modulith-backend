package com.aionn.inventory.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.inventory.domain.event.StockTransferEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.StockTransferStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class StockTransfer extends AggregateRoot {

    private final String transferId;
    private final String merchantId;
    private final String fromWarehouseId;
    private final String toWarehouseId;
    private final String skuId;
    private final int qty;
    private StockTransferStatus status;
    private final Instant initiatedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    public StockTransfer(
            String transferId,
            String merchantId,
            String fromWarehouseId,
            String toWarehouseId,
            String skuId,
            int qty,
            StockTransferStatus status,
            Instant initiatedAt,
            Instant completedAt,
            Instant cancelledAt) {
        this.transferId = transferId;
        this.merchantId = merchantId;
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        this.skuId = skuId;
        this.qty = qty;
        this.status = status;
        this.initiatedAt = initiatedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
    }

    public static StockTransfer initiate(
            String transferId,
            String merchantId,
            String fromWarehouseId,
            String toWarehouseId,
            String skuId,
            int qty) {
        return initiate(transferId, merchantId, fromWarehouseId, toWarehouseId, skuId, qty, Clock.systemUTC());
    }

    public static StockTransfer initiate(
            String transferId,
            String merchantId,
            String fromWarehouseId,
            String toWarehouseId,
            String skuId,
            int qty,
            Clock clock) {
        Guard.require(qty > 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "qty must be > 0"));
        Guard.require(!fromWarehouseId.equals(toWarehouseId),
                () -> new InventoryException(InventoryErrorCode.STOCK_TRANSFER_SAME_WAREHOUSE));
        Instant now = clock.instant();
        StockTransfer transfer = new StockTransfer(transferId, merchantId, fromWarehouseId, toWarehouseId,
                skuId, qty, StockTransferStatus.INITIATED, now, null, null);
        transfer.registerEvent(new StockTransferEvents.StockTransferInitiated(
                transferId, merchantId, fromWarehouseId, toWarehouseId, skuId, qty, now, now));
        return transfer;
    }

    public void complete(int receivedQty) {
        complete(receivedQty, Clock.systemUTC());
    }

    public void complete(int receivedQty, Clock clock) {
        Guard.require(status.canTransitionTo(StockTransferStatus.COMPLETED),
                () -> new InventoryException(InventoryErrorCode.STOCK_TRANSFER_INVALID,
                        "Transfer is not in INITIATED state"));
        Guard.require(receivedQty > 0 && receivedQty <= qty,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT,
                        "receivedQty must be in (0, " + qty + "]"));
        Instant now = clock.instant();
        this.status = StockTransferStatus.COMPLETED;
        this.completedAt = now;
        registerEvent(new StockTransferEvents.StockTransferCompleted(
                transferId, merchantId, fromWarehouseId, toWarehouseId, skuId, receivedQty, now, now));
    }

    public void cancel(String reason) {
        cancel(reason, Clock.systemUTC());
    }

    public void cancel(String reason, Clock clock) {
        Guard.require(status.canTransitionTo(StockTransferStatus.CANCELLED),
                () -> new InventoryException(InventoryErrorCode.STOCK_TRANSFER_INVALID,
                        "Transfer is not cancellable"));
        Instant now = clock.instant();
        this.status = StockTransferStatus.CANCELLED;
        this.cancelledAt = now;
        registerEvent(new StockTransferEvents.StockTransferCancelled(transferId, merchantId, reason, now, now));
    }

    @Override
    protected String aggregateId() {
        return transferId;
    }
}
