package com.aionn.chat.application.port.in.message;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;

public interface MarkDeliveredInputPort {
    MessageResult execute(MessageCommands.DeliverMessage command);
}
