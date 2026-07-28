package com.aionn.chat.application.port.in.conversation;

import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;

public interface StartConversationInputPort {
    ConversationResult execute(ConversationCommands.StartConversation command);
}
