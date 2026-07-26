package com.aionn.notification.infrastructure.listener;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDispatchService;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.sharedkernel.integration.event.identity.EmailChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PasswordChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PhoneChangedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventListener {

    private static final String EVENT_PASSWORD_CHANGED = "identity.password-changed";
    private static final String EVENT_EMAIL_CHANGED = "identity.email-changed";
    private static final String EVENT_PHONE_CHANGED = "identity.phone-changed";

    private final NotificationDispatchService dispatchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PasswordChangedIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        if (event.channelHint() != null) {
            context.put("channelHint", event.channelHint());
        }
        dispatch(event.userId(), EVENT_PASSWORD_CHANGED, context);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(EmailChangedIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        if (event.oldEmail() != null) {
            context.put("oldEmail", event.oldEmail());
        }
        if (event.newEmail() != null) {
            context.put("newEmail", event.newEmail());
        }
        dispatch(event.userId(), EVENT_EMAIL_CHANGED, context);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PhoneChangedIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        if (event.oldPhone() != null) {
            context.put("oldPhone", event.oldPhone());
        }
        if (event.newPhone() != null) {
            context.put("newPhone", event.newPhone());
        }
        dispatch(event.userId(), EVENT_PHONE_CHANGED, context);
    }

    private void dispatch(String userId, String eventType, Map<String, String> context) {
        try {
            dispatchService.sendByEvent(new NotificationCommands.SendByEvent(
                    userId, eventType, NotificationCategory.SECURITY,
                    null, null, null, context));
        } catch (RuntimeException ex) {
            log.warn("Notification dispatch failed for {}/{}: {}", userId, eventType, ex.getMessage());
        }
    }
}
