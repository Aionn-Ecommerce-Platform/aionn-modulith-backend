package com.aionn.chat.adapter.rest.dto.message.request;

import com.aionn.chat.domain.valueobject.MessageType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SendMessageRequest(
                @NotNull MessageType type,
                String body,
                Map<String, Object> metadata) {
}
