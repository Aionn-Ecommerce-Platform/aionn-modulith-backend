package com.aionn.chat.application.dto.message.result;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record MessageResult(
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

