package com.aionn.notification.adapter.rest.dto.notification;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SendNotificationRequest(
        @NotBlank String userId,
        @NotBlank String eventType,
        @NotNull NotificationCategory category,
        List<NotificationChannel> channels,
        String locale,
        String campaignId,
        Map<String, String> context) {
}

