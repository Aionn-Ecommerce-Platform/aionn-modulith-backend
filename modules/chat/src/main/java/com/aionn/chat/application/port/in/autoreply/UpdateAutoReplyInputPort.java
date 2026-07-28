package com.aionn.chat.application.port.in.autoreply;

import com.aionn.chat.application.dto.autoreply.command.AutoReplyCommands;
import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;

public interface UpdateAutoReplyInputPort {
    AutoReplyResult execute(AutoReplyCommands.UpdateAutoReply command);
}
