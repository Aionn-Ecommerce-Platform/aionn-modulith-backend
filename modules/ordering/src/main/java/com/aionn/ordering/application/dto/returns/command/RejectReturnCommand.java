package com.aionn.ordering.application.dto.returns.command;

import com.aionn.sharedkernel.application.command.Command;

public record RejectReturnCommand(
        String returnId,
        String ownerId,
        String reason) implements Command {
}
