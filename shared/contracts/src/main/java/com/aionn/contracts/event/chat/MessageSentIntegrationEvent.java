package com.aionn.contracts.event.chat;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record MessageSentIntegrationEvent(
        String eventId,
        String conversationId,
        String messageId,
        String senderId,
        String recipientId,
        String senderDisplayName,
        String messagePreview,
        Instant occurredAt) implements IntegrationEvent.ConversationScoped {

    public MessageSentIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
