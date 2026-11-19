package com.aionn.notification.domain.valueobject;

public enum NotificationCategory {
    ORDER,
    SECURITY,
    TRANSACTION,
    SHIPPING,
    PROMOTION,
    CHAT,
    SYSTEM;

    public boolean isMandatory() {
        return this == ORDER || this == SECURITY || this == TRANSACTION;
    }
}
