package com.aionn.inventory.application.dto.transfer.command;

import com.aionn.sharedkernel.application.command.Command;

public record CancelTransferCommand(
        String ownerId,
        String transferId,
        String reason) implements Command {
}
