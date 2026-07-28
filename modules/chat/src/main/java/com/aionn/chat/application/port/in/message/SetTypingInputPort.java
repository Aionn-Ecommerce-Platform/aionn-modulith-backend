package com.aionn.chat.application.port.in.message;

import com.aionn.chat.application.dto.message.command.MessageCommands;

public interface SetTypingInputPort {
    void execute(MessageCommands.SetTyping command);
}
