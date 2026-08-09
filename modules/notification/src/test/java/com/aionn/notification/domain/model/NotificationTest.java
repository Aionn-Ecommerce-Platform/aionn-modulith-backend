package com.aionn.notification.domain.model;

import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationPriority;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-05T10:00:00Z"), ZoneOffset.UTC);

    private static Notification newNotification() {
        return Notification.create("noti-1", "user-1", "tpl-1",
                NotificationChannel.EMAIL, NotificationCategory.TRANSACTION,
                "Subject", "Content", "campaign-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
    }

    @Test
    void createsPendingNotificationWithPriorityFromCategory() {
        Notification n = newNotification();

        assertThat(n.getNotiId()).isEqualTo("noti-1");
        assertThat(n.getUserId()).isEqualTo("user-1");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(n.getCategory()).isEqualTo(NotificationCategory.TRANSACTION);
        assertThat(n.getRetryCount()).isZero();
    }

    @Test
    void markSentTransitionsAndEmitsEvent() {
        Notification n = newNotification();

        n.markSent(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
        assertThat(n.peekEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.NotificationSent);
    }

    @Test
    void markFailedIncrementsRetryCount() {
        Notification n = newNotification();

        n.markFailed("smtp-down", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        n.markFailed("smtp-down", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(n.getRetryCount()).isEqualTo(2);
        assertThat(n.getLastFailureReason()).isEqualTo("smtp-down");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.canRetry()).isTrue();
    }

    @Test
    void markFailedAtMaxRetryTransitionsToFailed() {
        Notification n = newNotification();

        n.markFailed("err", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        n.markFailed("err", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        n.markFailed("err", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(n.getRetryCount()).isEqualTo(3);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.canRetry()).isFalse();
    }

    @Test
    void markDeliveryUnknownFailsImmediatelyWithoutEmittingAnEvent() {
        Notification n = newNotification();

        n.markDeliveryUnknown("gateway timeout", FIXED_CLOCK);

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getRetryCount()).isEqualTo(1);
        assertThat(n.getLastFailureReason()).isEqualTo("DELIVERY_OUTCOME_UNKNOWN:gateway timeout");
        assertThat(n.canRetry()).isFalse();
        // No failure event: the provider may already have delivered the message, so
        // re-driving it from an event would risk a duplicate send.
        assertThat(n.pullEvents()).isEmpty();
    }

    @Test
    void markDeliveryUnknownIsRejectedOnceTheNotificationIsRead() {
        Notification n = newNotification();
        n.markSent(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        n.markRead(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> n.markDeliveryUnknown("gateway timeout", FIXED_CLOCK))
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_INVALID_STATE.getCode());
    }

    @Test
    void markReadAfterSentTransitionsAndIsIdempotent() {
        Notification n = newNotification();
        n.markSent(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        n.markRead(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        n.markRead(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void softDeleteIsIdempotent() {
        Notification n = newNotification();

        n.softDelete(FIXED_CLOCK);
        Instant firstDeletedAt = n.getDeletedAt();
        n.softDelete(FIXED_CLOCK);

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.DELETED);
        assertThat(n.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void markReadFromPendingThrows() {
        Notification n = newNotification();

        assertThatThrownBy(() -> n.markRead(
                java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_INVALID_STATE.getCode());
    }

    @Test
    void softDeleteFromAnyNonDeletedStateAllowed() {
        Notification n = newNotification();

        n.softDelete(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.DELETED);
        assertThat(n.getDeletedAt()).isNotNull();
    }

    @Test
    void ensureOwnedByOtherUserThrows() {
        Notification n = newNotification();

        assertThatThrownBy(() -> n.ensureOwnedBy("OTHER"))
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_FORBIDDEN.getCode());
    }
}
