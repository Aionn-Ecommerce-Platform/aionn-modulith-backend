package com.aionn.identity.domain.valueobject;

public enum KycStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(KycStatus newStatus) {
        return switch (this) {
            case DRAFT -> newStatus == SUBMITTED || newStatus == CANCELLED;
            case SUBMITTED -> newStatus == IN_REVIEW || newStatus == APPROVED || newStatus == REJECTED
                    || newStatus == CANCELLED;
            case IN_REVIEW -> newStatus == APPROVED || newStatus == REJECTED;
            case REJECTED -> newStatus == DRAFT || newStatus == SUBMITTED;
            case APPROVED, CANCELLED -> false;
        };
    }

    public boolean canBeCancelled() {
        return this == DRAFT || this == SUBMITTED;
    }
}
