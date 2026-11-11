package com.aionn.notification.infrastructure.realtime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StompNotificationRealtimeAdapterTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StompNotificationRealtimeAdapter adapter =
            new StompNotificationRealtimeAdapter(messagingTemplate);

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void sendsCreatedEventToOnlyTheTargetUsersQueue() {
        Instant createdAt = Instant.parse("2026-11-08T00:00:00Z");

        adapter.publishCreated("user-1", "noti-1", createdAt);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(eq("user-1"), eq("/queue/notifications"), payload.capture());
        assertThat(payload.getValue()).isEqualTo(Map.of(
                "type", "NOTIFICATION_CREATED",
                "notiId", "noti-1",
                "createdAt", createdAt.toString()));
    }

    @Test
    void waitsUntilTheTransactionCommitsBeforePublishing() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        Instant createdAt = Instant.parse("2026-11-08T00:00:00Z");

        adapter.publishCreated("user-1", "noti-1", createdAt);
        verifyNoInteractions(messagingTemplate);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(messagingTemplate).convertAndSendToUser(eq("user-1"), eq("/queue/notifications"), any());
    }
}
