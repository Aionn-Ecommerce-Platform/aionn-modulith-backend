package com.aionn.chat.adapter.rest.dto.conversation.response;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
        String conversationId,
        String buyerId,
        String merchantId,
        List<ParticipantResponse> participants,
        String lastMessageId,
        String lastMessagePreview,
        String lastMessageType,
        String lastMessageSenderId,
        Instant lastMessageAt,
        boolean archived,
        long unreadCount,
        Instant createdAt,
        Instant updatedAt) {

    public record ParticipantResponse(
            String userId,
            String role,
            String displayName,
            String avatarUrl,
            Instant joinedAt,
            Instant lastReadAt) {
    }
}
