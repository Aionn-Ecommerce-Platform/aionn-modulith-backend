package com.aionn.notification.infrastructure.listener;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.sharedkernel.integration.event.chat.MessageSentIntegrationEvent;
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
public class ChatEventListener {

    private static final String EVENT_MESSAGE_RECEIVED = "chat.message-received";

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MessageSentIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("conversationId", event.conversationId());
        context.put("messageId", event.messageId());
        context.put("senderId", event.senderId());
        context.put("senderName", nullToEmpty(event.senderDisplayName()));
        context.put("messagePreview", nullToEmpty(event.messagePreview()));
        try {
            deliveryOrchestrator.sendByEvent(new NotificationCommands.SendByEvent(
                    event.recipientId(), EVENT_MESSAGE_RECEIVED, NotificationCategory.CHAT,
                    null, null, null, context));
        } catch (RuntimeException ex) {
            log.warn("Chat fan-out dispatch failed for recipient={} message={}: {}",
                    event.recipientId(), event.messageId(), ex.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
