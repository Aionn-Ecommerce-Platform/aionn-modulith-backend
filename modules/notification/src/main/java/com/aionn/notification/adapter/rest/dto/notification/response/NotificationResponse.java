package com.aionn.notification.adapter.rest.dto.notification.response;

import java.time.Instant;

public record NotificationResponse(
        String notiId,
        String userId,
        String templateId,
        String channel,
        String category,
        String priority,
        String subject,
        String content,
        String campaignId,
        String status,
        int retryCount,
        String lastFailureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant sentAt,
        Instant readAt,
        Instant deletedAt) {
}
