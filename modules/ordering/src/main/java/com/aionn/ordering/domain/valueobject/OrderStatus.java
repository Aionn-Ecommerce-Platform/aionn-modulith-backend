package com.aionn.ordering.domain.valueobject;

/**
 * Order lifecycle: PENDING (awaiting reservation/payment) → APPROVED → PREPARING → SHIPPED →
 * COMPLETED, with CANCELLED/REJECTED as terminal alternatives. Used by the state machine below.
 */
public enum OrderStatus {
    PENDING,
    APPROVED,
    PREPARING,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == CANCELLED || next == REJECTED;
            case APPROVED -> next == PREPARING || next == CANCELLED || next == REJECTED;
            case PREPARING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == COMPLETED;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REJECTED;
    }

    public boolean isPickedUpByCarrier() {
        return this == SHIPPED || this == COMPLETED;
    }
}

