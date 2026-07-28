package com.aionn.chat.application.port.in.conversation;

import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;

public interface ArchiveConversationInputPort {
    ConversationResult execute(ConversationCommands.Archive command);
}
