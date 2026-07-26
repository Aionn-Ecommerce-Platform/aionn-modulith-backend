package com.aionn.notification.domain.model;

import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationPriority;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private static Notification newNotification() {
        return Notification.create("noti-1", "user-1", "tpl-1",
                NotificationChannel.EMAIL, NotificationCategory.TRANSACTION,
                "Subject", "Content", "campaign-1");
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

        n.markSent();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
        assertThat(n.peekEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.NotificationSent);
    }

    @Test
    void markFailedIncrementsRetryCount() {
        Notification n = newNotification();

        n.markFailed("smtp-down");
        n.markFailed("smtp-down");

        assertThat(n.getRetryCount()).isEqualTo(2);
        assertThat(n.getLastFailureReason()).isEqualTo("smtp-down");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.canRetry()).isTrue();
    }

    @Test
    void markFailedAtMaxRetryTransitionsToFailed() {
        Notification n = newNotification();

        n.markFailed("err");
        n.markFailed("err");
        n.markFailed("err");

        assertThat(n.getRetryCount()).isEqualTo(3);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.canRetry()).isFalse();
    }

    @Test
    void markReadAfterSentTransitionsAndIsIdempotent() {
        Notification n = newNotification();
        n.markSent();

        n.markRead();
        n.markRead();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void markReadFromPendingThrows() {
        Notification n = newNotification();

        assertThatThrownBy(n::markRead)
                .isInstanceOf(NotificationException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_INVALID_STATE.getCode());
    }

    @Test
    void softDeleteFromAnyNonDeletedStateAllowed() {
        Notification n = newNotification();

        n.softDelete();

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
