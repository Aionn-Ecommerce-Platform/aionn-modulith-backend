package com.aionn.inventory.domain.valueobject;

public enum StockTransferStatus {
    INITIATED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(StockTransferStatus next) {
        return switch (this) {
            case INITIATED -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

