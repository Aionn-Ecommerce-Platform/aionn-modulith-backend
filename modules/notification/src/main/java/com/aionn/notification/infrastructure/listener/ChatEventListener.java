package com.aionn.notification.infrastructure.listener;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.sharedkernel.integration.event.chat.MessageSentIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private static final String EVENT_MESSAGE_RECEIVED = "chat.message-received";

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;

    @EventListener
    public void on(MessageSentIntegrationEvent event) {
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("conversationId", event.conversationId());
        context.put("messageId", event.messageId());
        context.put("senderId", event.senderId());
        context.put("senderName", nullToEmpty(event.senderDisplayName()));
        context.put("messagePreview", nullToEmpty(event.messagePreview()));
        deliveryOrchestrator.sendByEvent(new NotificationCommands.SendByEvent(
                event.recipientId(), EVENT_MESSAGE_RECEIVED, NotificationCategory.CHAT,
                null, null, null, context));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
