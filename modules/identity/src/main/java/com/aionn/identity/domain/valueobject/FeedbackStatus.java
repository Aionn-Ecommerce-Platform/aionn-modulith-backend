package com.aionn.identity.domain.valueobject;

public enum FeedbackStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(FeedbackStatus newStatus) {
        if (newStatus == null || newStatus == this) {
            return true;
        }
        return switch (this) {
            case OPEN -> newStatus == IN_REVIEW || newStatus == RESOLVED || newStatus == CLOSED;
            case IN_REVIEW -> newStatus == OPEN || newStatus == RESOLVED || newStatus == CLOSED;
            case RESOLVED -> newStatus == OPEN || newStatus == IN_REVIEW || newStatus == CLOSED;
            case CLOSED -> false;
        };
    }
}
