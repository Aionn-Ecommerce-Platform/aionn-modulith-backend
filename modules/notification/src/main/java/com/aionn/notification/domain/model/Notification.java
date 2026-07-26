package com.aionn.notification.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationPriority;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class Notification extends AggregateRoot {

    private static final int MAX_RETRY = 3;

    private final String notiId;
    private final String userId;
    private final String templateId;
    private final NotificationChannel channel;
    private final NotificationCategory category;
    private final NotificationPriority priority;
    private final String subject;
    private final String content;
    private final String campaignId;
    private NotificationStatus status;
    private int retryCount;
    private String lastFailureReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant sentAt;
    private Instant readAt;
    private Instant deletedAt;

    public Notification(
            String notiId,
            String userId,
            String templateId,
            NotificationChannel channel,
            NotificationCategory category,
            NotificationPriority priority,
            String subject,
            String content,
            String campaignId,
            NotificationStatus status,
            int retryCount,
            String lastFailureReason,
            Instant createdAt,
            Instant updatedAt,
            Instant sentAt,
            Instant readAt,
            Instant deletedAt) {
        this.notiId = notiId;
        this.userId = userId;
        this.templateId = templateId;
        this.channel = channel;
        this.category = category;
        this.priority = priority;
        this.subject = subject;
        this.content = content;
        this.campaignId = campaignId;
        this.status = status;
        this.retryCount = retryCount;
        this.lastFailureReason = lastFailureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sentAt = sentAt;
        this.readAt = readAt;
        this.deletedAt = deletedAt;
    }

    public static Notification create(
            String notiId,
            String userId,
            String templateId,
            NotificationChannel channel,
            NotificationCategory category,
            String subject,
            String content,
            String campaignId) {
        return create(notiId, userId, templateId, channel, category, subject, content, campaignId,
                Clock.systemUTC());
    }

    public static Notification create(
            String notiId,
            String userId,
            String templateId,
            NotificationChannel channel,
            NotificationCategory category,
            String subject,
            String content,
            String campaignId,
            Clock clock) {
        Instant now = clock.instant();
        return new Notification(notiId, userId, templateId, channel, category,
                NotificationPriority.forCategory(category), subject, content, campaignId,
                NotificationStatus.PENDING, 0, null, now, now, null, null, null);
    }

    public void ensureOwnedBy(String userId) {
        Guard.require(this.userId.equals(userId),
                () -> new NotificationException(NotificationErrorCode.NOTIFICATION_FORBIDDEN));
    }

    public void markSent() {
        markSent(Clock.systemUTC());
    }

    public void markSent(Clock clock) {
        ensureTransition(NotificationStatus.SENT);
        Instant now = clock.instant();
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.updatedAt = now;
        registerEvent(new NotificationEvents.NotificationSent(notiId, userId, channel.name(), content, now));
    }

    public void markFailed(String reason) {
        markFailed(reason, Clock.systemUTC());
    }

    public void markFailed(String reason, Clock clock) {
        this.retryCount++;
        this.lastFailureReason = reason;
        Instant now = clock.instant();
        this.updatedAt = now;
        if (retryCount >= MAX_RETRY) {
            ensureTransition(NotificationStatus.FAILED);
            this.status = NotificationStatus.FAILED;
        }
        registerEvent(new NotificationEvents.NotificationFailed(notiId, reason, retryCount, now));
    }

    public boolean canRetry() {
        return status == NotificationStatus.PENDING && retryCount < MAX_RETRY;
    }

    public void markRead() {
        markRead(Clock.systemUTC());
    }

    public void markRead(Clock clock) {
        if (status == NotificationStatus.READ)
            return;
        ensureTransition(NotificationStatus.READ);
        Instant now = clock.instant();
        this.status = NotificationStatus.READ;
        this.readAt = now;
        this.updatedAt = now;
        registerEvent(new NotificationEvents.NotificationRead(notiId, userId, readAt, now));
    }

    public void softDelete() {
        softDelete(Clock.systemUTC());
    }

    public void softDelete(Clock clock) {
        if (status == NotificationStatus.DELETED)
            return;
        ensureTransition(NotificationStatus.DELETED);
        Instant now = clock.instant();
        this.status = NotificationStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
        registerEvent(new NotificationEvents.NotificationDeleted(notiId, userId, deletedAt, now));
    }

    private void ensureTransition(NotificationStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new NotificationException(NotificationErrorCode.NOTIFICATION_INVALID_STATE,
                        "Cannot transition notification from " + status + " to " + next));
    }

    @Override
    protected String aggregateId() {
        return notiId;
    }
}
