package com.aionn.notification.domain.valueobject;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    READ,
    DELETED;

    public boolean canTransitionTo(NotificationStatus next) {
        return switch (this) {
            case PENDING -> next == SENT || next == FAILED || next == DELETED;
            case SENT -> next == READ || next == DELETED;
            case READ -> next == DELETED;
            case FAILED -> next == DELETED;
            case DELETED -> false;
        };
    }
}

