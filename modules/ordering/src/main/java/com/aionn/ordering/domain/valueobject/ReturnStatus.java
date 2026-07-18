package com.aionn.ordering.domain.valueobject;

/**
 * Lifecycle of an OrderReturn aggregate.
 *
 * <pre>
 *   REQUESTED â†’ APPROVED â†’ ITEM_RECEIVED   (then refund issues elsewhere)
 *   REQUESTED â†’ REJECTED
 *   APPROVED  â†’ REJECTED                   (lost in transit / wrong item)
 * </pre>
 */
public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    ITEM_RECEIVED,
    REJECTED;

    public boolean canTransitionTo(ReturnStatus next) {
        return switch (this) {
            case REQUESTED -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == ITEM_RECEIVED || next == REJECTED;
            case ITEM_RECEIVED, REJECTED -> false;
        };
    }
}

