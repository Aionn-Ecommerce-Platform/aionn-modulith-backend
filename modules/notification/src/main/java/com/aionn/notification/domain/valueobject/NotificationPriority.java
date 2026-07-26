package com.aionn.notification.domain.valueobject;

public enum NotificationPriority {

    CRITICAL,

    HIGH,

    NORMAL,

    LOW;

    public static NotificationPriority forCategory(NotificationCategory category) {
        return switch (category) {
            case SECURITY -> CRITICAL;
            case TRANSACTION, SHIPPING -> HIGH;
            case CHAT, SYSTEM -> NORMAL;
            case PROMOTION -> LOW;
        };
    }
}
