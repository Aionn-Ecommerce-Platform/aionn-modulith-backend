package com.aionn.notification.application.dto.notification.result;

import java.time.Instant;

public record NotificationResult(
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

