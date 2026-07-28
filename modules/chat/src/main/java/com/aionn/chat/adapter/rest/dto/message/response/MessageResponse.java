package com.aionn.chat.adapter.rest.dto.message.response;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record MessageResponse(
        String messageId,
        String conversationId,
        String senderId,
        String senderRole,
        String type,
        String body,
        Map<String, Object> metadata,
        String status,
        Set<String> deliveredTo,
        Set<String> readBy,
        boolean recalled,
        Instant sentAt,
        Instant updatedAt) {
}
