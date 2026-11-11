package com.aionn.notification.infrastructure.realtime;

import com.aionn.notification.application.port.out.NotificationRealtimePort;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompNotificationRealtimeAdapter implements NotificationRealtimePort {

    private static final String DESTINATION = "/queue/notifications";
    private static final String EVENT_TYPE = "NOTIFICATION_CREATED";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishCreated(String userId, String notificationId, Instant createdAt) {
        Runnable publish = () -> {
            try {
                messagingTemplate.convertAndSendToUser(userId, DESTINATION, Map.of(
                        "type", EVENT_TYPE,
                        "notiId", notificationId,
                        "createdAt", createdAt.toString()));
            } catch (RuntimeException exception) {
                // The notification is already persisted; realtime delivery is best-effort and
                // clients can recover through the inbox API and its polling fallback.
                log.warn("Could not publish realtime notification event for notification {}", notificationId,
                        exception);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
