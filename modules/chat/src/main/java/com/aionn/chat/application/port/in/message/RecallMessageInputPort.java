package com.aionn.chat.application.port.in.message;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;

public interface RecallMessageInputPort {
    MessageResult execute(MessageCommands.RecallMessage command);
}
