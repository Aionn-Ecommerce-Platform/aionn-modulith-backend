package com.aionn.notification.domain.valueobject;

public enum NotificationCategory {
    SECURITY,
    TRANSACTION,
    SHIPPING,
    PROMOTION,
    CHAT,
    SYSTEM;

    public boolean isMandatory() {
        return this == SECURITY || this == TRANSACTION;
    }
}
