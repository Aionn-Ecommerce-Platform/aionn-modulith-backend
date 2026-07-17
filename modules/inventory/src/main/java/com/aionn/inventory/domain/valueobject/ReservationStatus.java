package com.aionn.inventory.domain.valueobject;

/**
 * Stock reservation lifecycle.
 *
 * <pre>
 *   RESERVED â†’ COMMITTED   (PaymentSucceeded â†’ physical decrement)
 *   RESERVED â†’ RELEASED    (Order cancelled or expired)
 *   RESERVED â†’ FAILED      (insufficient stock at create time)
 * </pre>
 */
public enum ReservationStatus {
    RESERVED,
    COMMITTED,
    RELEASED,
    FAILED;

    public boolean canTransitionTo(ReservationStatus next) {
        return switch (this) {
            case RESERVED -> next == COMMITTED || next == RELEASED;
            case COMMITTED, RELEASED, FAILED -> false;
        };
    }

    public boolean isTerminal() {
        return this != RESERVED;
    }
}

