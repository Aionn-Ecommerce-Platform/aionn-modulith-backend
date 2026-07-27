package com.aionn.notification.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode {
    NOTIFICATION_NOT_FOUND("NTF_001", "Notification not found"),
    NOTIFICATION_FORBIDDEN("NTF_002", "Notification does not belong to this user"),
    NOTIFICATION_INVALID_STATE("NTF_003", "Notification is not in a state that allows this action"),

    TEMPLATE_NOT_FOUND("NTF_101", "Notification template not found"),
    TEMPLATE_DUPLICATE("NTF_102", "Notification template already exists for that event"),
    TEMPLATE_PLACEHOLDER_MISSING("NTF_103", "Required placeholder missing from context"),

    SUBSCRIPTION_NOT_FOUND("NTF_201", "Subscription not found"),
    SUBSCRIPTION_REQUIRED_CHANNEL("NTF_202", "Cannot disable a required channel (security/transaction)"),

    PROVIDER_NOT_FOUND("NTF_301", "Notification provider not found"),
    PROVIDER_NOT_ACTIVE("NTF_302", "Notification provider is not active"),

    DEVICE_TOKEN_NOT_FOUND("NTF_401", "Device token not found"),
    RECIPIENT_NOT_FOUND("NTF_402", "Notification recipient not found"),

    INVALID_ARGUMENT("NTF_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}
