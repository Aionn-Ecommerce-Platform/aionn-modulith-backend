package com.aionn.sharedkernel.integration.event.chat;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record MessageSentIntegrationEvent(
        String eventId,
        String conversationId,
        String messageId,
        String senderId,
        String recipientId,
        String senderDisplayName,
        String messagePreview,
        Instant occurredAt) implements IntegrationEvent {

    public MessageSentIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
