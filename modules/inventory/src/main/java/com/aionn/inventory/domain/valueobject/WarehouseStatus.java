package com.aionn.inventory.domain.valueobject;

/**
 * Warehouse lifecycle.
 *
 * <pre>
 *   ACTIVE   â‡„ INACTIVE   (merchant toggles availability)
 *   ACTIVE   â†’  SUSPENDED (CS Admin emergency stop)
 *   SUSPENDED â†’ ACTIVE    (CS Admin lifts the suspension)
 * </pre>
 */
public enum WarehouseStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    public boolean canTransitionTo(WarehouseStatus next) {
        return switch (this) {
            case ACTIVE -> next == INACTIVE || next == SUSPENDED;
            case INACTIVE -> next == ACTIVE || next == SUSPENDED;
            case SUSPENDED -> next == ACTIVE;
        };
    }

    public boolean canFulfill() {
        return this == ACTIVE;
    }
}

