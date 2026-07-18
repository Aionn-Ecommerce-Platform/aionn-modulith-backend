package com.aionn.ordering.application.dto.returns.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfirmItemReceivedCommand(
        String returnId,
        String ownerId,
        String itemCondition) implements Command {
}
