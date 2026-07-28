package com.aionn.chat.application.dto.conversation.command;

import com.aionn.sharedkernel.application.command.Command;

public final class ConversationCommands {

    private ConversationCommands() {
    }

    public record StartConversation(
            String buyerId,
            String buyerDisplayName,
            String buyerAvatarUrl,
            String merchantId,
            String merchantDisplayName,
            String merchantAvatarUrl,
            String startedBy) implements Command {
    }

    public record MarkRead(String userId, String conversationId) implements Command {
    }

    public record Archive(String userId, String conversationId) implements Command {
    }

    public record Unarchive(String userId, String conversationId) implements Command {
    }

    public record JoinSupport(String supportUserId, String conversationId, String displayName, String avatarUrl)
            implements Command {
    }
}
