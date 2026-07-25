package com.aionn.shipping.domain.valueobject;

public enum ShipmentStatus {
    REQUESTED,
    REGISTERED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNED,
    CANCELLED;

    /**
     * Allow forward progression so the polling worker can jump straight to the
     * latest carrier-reported state without driving every intermediate step.
     */
    public boolean canTransitionTo(ShipmentStatus next) {
        if (this == next) {
            return false;
        }
        if (this == DELIVERED || this == RETURNED || this == CANCELLED) {
            return false;
        }
        return switch (next) {
            case REQUESTED -> false;
            case REGISTERED -> this == REQUESTED;
            case PICKED_UP -> this == REQUESTED || this == REGISTERED;
            case IN_TRANSIT -> this == REGISTERED || this == PICKED_UP;
            case OUT_FOR_DELIVERY -> this != OUT_FOR_DELIVERY;
            case DELIVERY_FAILED -> this == OUT_FOR_DELIVERY || this == IN_TRANSIT
                    || this == PICKED_UP;
            case DELIVERED -> this == OUT_FOR_DELIVERY || this == IN_TRANSIT
                    || this == PICKED_UP || this == DELIVERY_FAILED;
            case RETURNED -> true;
            case CANCELLED -> this == REQUESTED || this == REGISTERED;
        };
    }

    public boolean isPickedUp() {
        return this != REQUESTED && this != REGISTERED && this != CANCELLED;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }
}
