package com.aionn.chat.application.dto.conversation.result;

import java.time.Instant;
import java.util.List;

public record ConversationResult(
        String conversationId,
        String buyerId,
        String merchantId,
        List<ParticipantResult> participants,
        String lastMessageId,
        String lastMessagePreview,
        String lastMessageType,
        String lastMessageSenderId,
        Instant lastMessageAt,
        boolean archived,
        long unreadCount,
        Instant createdAt,
        Instant updatedAt) {

    public record ParticipantResult(
            String userId,
            String role,
            String displayName,
            String avatarUrl,
            Instant joinedAt,
            Instant lastReadAt) {
    }
}

