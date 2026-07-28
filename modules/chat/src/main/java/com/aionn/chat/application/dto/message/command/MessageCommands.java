package com.aionn.chat.application.dto.message.command;

import com.aionn.chat.domain.valueobject.MessageType;
import com.aionn.sharedkernel.application.command.Command;

import java.util.Map;

public final class MessageCommands {

    private MessageCommands() {
    }

    public record SendMessage(
            String senderId,
            String conversationId,
            MessageType type,
            String body,
            Map<String, Object> metadata) implements Command {
    }

    public record DeliverMessage(String userId, String messageId) implements Command {
    }

    public record ReadMessage(String userId, String messageId) implements Command {
    }

    public record RecallMessage(String userId, String messageId) implements Command {
    }

    public record SetTyping(String userId, String conversationId, boolean typing) implements Command {
    }
}
