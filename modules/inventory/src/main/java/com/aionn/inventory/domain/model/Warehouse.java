package com.aionn.inventory.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.inventory.domain.event.WarehouseEvents;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class Warehouse extends AggregateRoot {

    private final String warehouseId;
    private final String merchantId;
    private String address;
    private int priorityLevel;
    private WarehouseStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Warehouse(
            String warehouseId,
            String merchantId,
            String address,
            int priorityLevel,
            WarehouseStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.warehouseId = warehouseId;
        this.merchantId = merchantId;
        this.address = address;
        this.priorityLevel = priorityLevel;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Warehouse create(String warehouseId, String merchantId, String address, int priorityLevel) {
        return create(warehouseId, merchantId, address, priorityLevel, Clock.systemUTC());
    }

    public static Warehouse create(String warehouseId, String merchantId, String address, int priorityLevel, Clock clock) {
        Guard.require(merchantId != null && !merchantId.isBlank(),
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "merchantId must not be blank"));
        Guard.require(priorityLevel >= 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "priorityLevel must be >= 0"));
        Instant now = clock.instant();
        Warehouse warehouse = new Warehouse(warehouseId, merchantId, address, priorityLevel,
                WarehouseStatus.ACTIVE, now, now);
        warehouse.registerEvent(new WarehouseEvents.WarehouseCreated(
                warehouseId, merchantId, address, WarehouseStatus.ACTIVE.name(), now));
        return warehouse;
    }

    public void ensureOwnedBy(String merchantId) {
        Guard.require(this.merchantId.equals(merchantId),
                () -> new InventoryException(InventoryErrorCode.WAREHOUSE_FORBIDDEN));
    }

    public void changeStatus(WarehouseStatus next) {
        changeStatus(next, Clock.systemUTC());
    }

    public void changeStatus(WarehouseStatus next, Clock clock) {
        Guard.require(next != WarehouseStatus.SUSPENDED,
                () -> new InventoryException(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION,
                        "Use suspend(adminId, reason) for SUSPENDED"));
        Guard.require(status.canTransitionTo(next),
                () -> new InventoryException(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION,
                        "Cannot transition warehouse from " + status + " to " + next));
        this.status = next;
        touch(clock);
        registerEvent(new WarehouseEvents.WarehouseStatusChanged(warehouseId, next.name(), updatedAt));
    }

    public void adjustPriority(int newPriority) {
        adjustPriority(newPriority, Clock.systemUTC());
    }

    public void adjustPriority(int newPriority, Clock clock) {
        Guard.require(newPriority >= 0,
                () -> new InventoryException(InventoryErrorCode.INVALID_ARGUMENT, "priorityLevel must be >= 0"));
        this.priorityLevel = newPriority;
        touch(clock);
        registerEvent(new WarehouseEvents.WarehousePriorityAdjusted(warehouseId, merchantId, newPriority, updatedAt,
                updatedAt));
    }

    public void suspend(String adminId, String reason) {
        suspend(adminId, reason, Clock.systemUTC());
    }

    public void suspend(String adminId, String reason, Clock clock) {
        Guard.require(status.canTransitionTo(WarehouseStatus.SUSPENDED),
                () -> new InventoryException(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION,
                        "Cannot suspend a " + status + " warehouse"));
        this.status = WarehouseStatus.SUSPENDED;
        touch(clock);
        registerEvent(new WarehouseEvents.WarehouseSuspended(warehouseId, adminId, reason, updatedAt, updatedAt));
    }

    public void liftSuspension() {
        liftSuspension(Clock.systemUTC());
    }

    public void liftSuspension(Clock clock) {
        Guard.require(status == WarehouseStatus.SUSPENDED,
                () -> new InventoryException(InventoryErrorCode.WAREHOUSE_INVALID_TRANSITION,
                        "Warehouse is not suspended"));
        this.status = WarehouseStatus.ACTIVE;
        touch(clock);
        registerEvent(new WarehouseEvents.WarehouseStatusChanged(warehouseId, status.name(), updatedAt));
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    @Override
    protected String aggregateId() {
        return warehouseId;
    }
}
