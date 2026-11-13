package com.aionn.notification.adapter.rest.dto.subscription.request;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionRequest(
        @NotNull NotificationCategory category,
        @NotNull NotificationChannel channel,
        boolean enabled) {
}

