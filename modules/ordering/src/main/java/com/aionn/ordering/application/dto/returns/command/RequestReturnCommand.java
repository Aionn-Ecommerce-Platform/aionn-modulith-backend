package com.aionn.ordering.application.dto.returns.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestReturnCommand(
        String orderId,
        String userId,
        String reason,
        String evidenceUrl) implements Command {
}
