package com.aionn.chat.domain.event;

import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.chat.domain.valueobject.ParticipantRole;

import java.time.Instant;
import java.util.List;

public final class ChatEvents {

    private ChatEvents() {
    }

    public record ConversationStarted(
            String conversationId,
            List<String> participantIds,
            String startedBy,
            Instant occurredAt) implements ChatEvent {
    }

    public record MessageSent(
            String conversationId,
            String messageId,
            String senderId,
            ParticipantRole senderRole,
            List<String> recipientIds,
            MessageType type,
            MessagePayload payload,
            Instant occurredAt) implements ChatEvent {
    }

    public record MessageDelivered(
            String conversationId,
            String messageId,
            String recipientId,
            Instant occurredAt) implements ChatEvent {
    }

    public record MessageRead(
            String conversationId,
            String messageId,
            String readerId,
            Instant occurredAt) implements ChatEvent {
    }

    public record MessageRecalled(
            String conversationId,
            String messageId,
            String senderId,
            Instant occurredAt) implements ChatEvent {
    }

    public record ConversationRead(
            String conversationId,
            String userId,
            Instant readAt,
            Instant occurredAt) implements ChatEvent {
    }

    public record TypingIndicatorChanged(
            String conversationId,
            String userId,
            boolean typing,
            Instant occurredAt) implements ChatEvent {
    }

    public record UserBlocked(
            String blockerId,
            String blockedId,
            Instant occurredAt) implements ChatEvent {
    }

    public record UserUnblocked(
            String blockerId,
            String blockedId,
            Instant occurredAt) implements ChatEvent {
    }

    public record AutoReplyConfigured(
            String merchantId,
            boolean enabled,
            Instant occurredAt) implements ChatEvent {
    }
}

