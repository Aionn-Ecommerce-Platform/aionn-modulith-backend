package com.aionn.notification.domain.event;

import java.time.Instant;
import java.util.List;

public final class NotificationEvents {

    private NotificationEvents() {
    }

    public record NotificationSent(
            String notiId,
            String userId,
            String channel,
            String content,
            Instant occurredAt) implements NotificationEvent {
    }

    public record NotificationFailed(
            String notiId,
            String reason,
            int retryCount,
            Instant occurredAt) implements NotificationEvent {
    }

    public record NotificationRead(
            String notiId,
            String userId,
            Instant readAt,
            Instant occurredAt) implements NotificationEvent {
    }

    public record NotificationDeleted(
            String notiId,
            String userId,
            Instant deletedAt,
            Instant occurredAt) implements NotificationEvent {
    }

    public record TemplateCreated(
            String templateId,
            String type,
            String content,
            List<String> placeholders,
            Instant occurredAt) implements NotificationEvent {
    }

    public record TemplateUpdated(
            String templateId,
            String content,
            int version,
            Instant occurredAt) implements NotificationEvent {
    }

    public record SubscriptionUpdated(
            String userId,
            String settingsKey,
            String channel,
            boolean enabled,
            Instant occurredAt) implements NotificationEvent {
    }

    public record DeviceTokenRegistered(
            String userId,
            String deviceToken,
            String os,
            Instant registeredAt,
            Instant occurredAt) implements NotificationEvent {
    }

    public record ProviderConfigured(
            String providerId,
            String providerType,
            boolean active,
            String configuredBy,
            Instant updatedAt,
            Instant occurredAt) implements NotificationEvent {
    }

    public record AnalyticsReportGenerated(
            String reportId,
            String campaignId,
            int sentCount,
            int readCount,
            int failedCount,
            Instant generatedAt,
            Instant occurredAt) implements NotificationEvent {
    }
}

