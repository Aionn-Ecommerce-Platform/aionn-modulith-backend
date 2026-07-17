package com.aionn.inventory.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.AdjustmentType;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Getter
public class InventoryItem extends AggregateRoot {

    private final InventoryItemKey key;
    private int physicalQty;
    private int availableQty;
    private int safetyStockQty;
    private boolean locked;
    private String batchNo;
    private LocalDate expiryDate;
    private final Instant createdAt;
    private Instant updatedAt;

    public InventoryItem(
            InventoryItemKey key,
            int physicalQty,
            int availableQty,
            int safetyStockQty,
            boolean locked,
            String batchNo,
            LocalDate expiryDate,
            Instant createdAt,
            Instant updatedAt) {
        this.key = key;
        this.physicalQty = physicalQty;
        this.availableQty = availableQty;
        this.safetyStockQty = safetyStockQty;
        this.locked = locked;
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InventoryItem initialize(InventoryItemKey key, int initialQty) {
        return initialize(key, initialQty, Clock.systemUTC());
    }

    public static InventoryItem initialize(InventoryItemKey key, int initialQty, Clock clock) {
        Guard.require(initialQty >= 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "initialQty must be >= 0"));
        Instant now = clock.instant();
        InventoryItem item = new InventoryItem(key, initialQty, initialQty, 0,
                false, null, null, now, now);
        item.registerEvent(new InventoryItemEvents.StockInitialized(
                key.skuId(), key.warehouseId(), initialQty, now));
        return item;
    }

    public int reservedQty() {
        return physicalQty - availableQty;
    }

    public void reserve(int qty) {
        reserve(qty, Clock.systemUTC());
    }

    public void reserve(int qty, Clock clock) {
        ensurePositive(qty);
        ensureUnlocked();
        Guard.require(availableQty >= qty,
                () -> new InventoryException(InventoryErrorCode.INVENTORY_INSUFFICIENT_STOCK,
                        "Available " + availableQty + " < requested " + qty));
        this.availableQty -= qty;
        touch(clock);
    }

    public void commit(int qty) {
        commit(qty, Clock.systemUTC());
    }

    public void commit(int qty, Clock clock) {
        ensurePositive(qty);
        Guard.require(physicalQty >= qty && reservedQty() >= qty,
                () -> new InventoryException(InventoryErrorCode.INVENTORY_NEGATIVE_PHYSICAL_STOCK,
                        "Cannot commit " + qty + ": physical=" + physicalQty + ", reserved=" + reservedQty()));
        this.physicalQty -= qty;
        touch(clock);
    }

    public void release(int qty) {
        release(qty, Clock.systemUTC());
    }

    public void release(int qty, Clock clock) {
        ensurePositive(qty);
        Guard.require(reservedQty() >= qty,
                () -> new InventoryException(InventoryErrorCode.STOCK_RESERVATION_INVALID_STATE,
                        "Cannot release more than reserved"));
        this.availableQty += qty;
        touch(clock);
    }

    public void adjust(int signedDelta, AdjustmentType type, String reason) {
        adjust(signedDelta, type, reason, Clock.systemUTC());
    }

    public void adjust(int signedDelta, AdjustmentType type, String reason, Clock clock) {
        ensureUnlocked();
        if (signedDelta == 0) {
            return;
        }
        int newPhysical = physicalQty + signedDelta;
        int newAvailable = availableQty + signedDelta;
        Guard.require(newPhysical >= 0,
                () -> new InventoryException(InventoryErrorCode.INVENTORY_NEGATIVE_PHYSICAL_STOCK));
        Guard.require(newAvailable >= 0,
                () -> new InventoryException(InventoryErrorCode.INVENTORY_INSUFFICIENT_STOCK,
                        "Available would go below zero"));
        this.physicalQty = newPhysical;
        this.availableQty = newAvailable;
        touch(clock);
        registerEvent(new InventoryItemEvents.StockAdjusted(
                key.skuId(), key.warehouseId(), signedDelta, type.name() + ":" + (reason == null ? "" : reason),
                updatedAt));
    }

    public void configureSafetyStock(int safetyQty) {
        configureSafetyStock(safetyQty, Clock.systemUTC());
    }

    public void configureSafetyStock(int safetyQty, Clock clock) {
        Guard.require(safetyQty >= 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "safetyStockQty must be >= 0"));
        this.safetyStockQty = safetyQty;
        touch(clock);
        registerEvent(new InventoryItemEvents.SafetyStockConfigured(
                key.skuId(), key.warehouseId(), safetyQty, updatedAt, updatedAt));
        emitBreachIfApplicable(clock);
    }

    public void emitBreachIfApplicable() {
        emitBreachIfApplicable(Clock.systemUTC());
    }

    public void emitBreachIfApplicable(Clock clock) {
        if (safetyStockQty > 0 && availableQty < safetyStockQty) {
            registerEvent(new InventoryItemEvents.SafetyStockBreached(
                    key.skuId(), key.warehouseId(), availableQty, safetyStockQty, clock.instant()));
        }
    }

    public void emergencyLock(String adminId, String reason) {
        emergencyLock(adminId, reason, Clock.systemUTC());
    }

    public void emergencyLock(String adminId, String reason, Clock clock) {
        this.locked = true;
        touch(clock);
        registerEvent(new InventoryItemEvents.StockEmergencyLocked(
                key.skuId(), key.warehouseId(), adminId, reason, updatedAt, updatedAt));
    }

    public void emergencyUnlock(String adminId) {
        emergencyUnlock(adminId, Clock.systemUTC());
    }

    public void emergencyUnlock(String adminId, Clock clock) {
        this.locked = false;
        touch(clock);
        registerEvent(new InventoryItemEvents.StockEmergencyUnlocked(
                key.skuId(), key.warehouseId(), adminId, updatedAt, updatedAt));
    }

    public void trackBatchAndExpiry(String batchNo, LocalDate expiryDate) {
        trackBatchAndExpiry(batchNo, expiryDate, Clock.systemUTC());
    }

    public void trackBatchAndExpiry(String batchNo, LocalDate expiryDate, Clock clock) {
        Guard.require(expiryDate == null || !expiryDate.isBefore(LocalDate.now(clock.withZone(java.time.ZoneOffset.UTC))),
                () -> new InventoryException(InventoryErrorCode.INVENTORY_EXPIRY_INVALID,
                        "expiryDate must not be in the past"));
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        touch(clock);
        registerEvent(new InventoryItemEvents.BatchAndExpiryTracked(
                key.skuId(), key.warehouseId(), batchNo, expiryDate, updatedAt));
    }

    public void recordAudit(String auditId, int actualQty) {
        recordAudit(auditId, actualQty, Clock.systemUTC());
    }

    public void recordAudit(String auditId, int actualQty, Clock clock) {
        Guard.require(actualQty >= 0,
                () -> new InventoryException(InventoryErrorCode.INVENTORY_AUDIT_NEGATIVE));
        int expected = physicalQty;
        Instant now = clock.instant();
        registerEvent(new InventoryItemEvents.InventoryAudited(
                auditId, key.skuId(), key.warehouseId(), expected, actualQty, now, now));
        int delta = actualQty - expected;
        if (delta != 0) {
            int newAvailable = Math.max(0, availableQty + delta);
            this.physicalQty = actualQty;
            this.availableQty = Math.min(actualQty, newAvailable);
            touch(clock);
        }
    }

    private void ensurePositive(int qty) {
        Guard.require(qty > 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "qty must be > 0"));
    }

    private void ensureUnlocked() {
        Guard.require(!locked, () -> new InventoryException(InventoryErrorCode.INVENTORY_LOCKED));
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    @Override
    protected String aggregateId() {
        return key.skuId() + ":" + key.warehouseId();
    }
}
