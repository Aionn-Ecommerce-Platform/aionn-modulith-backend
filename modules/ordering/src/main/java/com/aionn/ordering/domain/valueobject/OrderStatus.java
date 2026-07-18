package com.aionn.ordering.domain.valueobject;

/**
 * Order lifecycle as derived from the EventStorming spec.
 *
 * <pre>
 *   PENDING        - just placed, awaiting reservation/payment
 *   APPROVED       - reservations + payment all confirmed
 *   PREPARING      - merchant accepted, packing
 *   SHIPPED        - handed off to carrier
 *   COMPLETED      - delivered
 *   CANCELLED      - by user / merchant / auto / system
 *   REJECTED       - merchant rejected (out of stock surprise)
 * </pre>
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

