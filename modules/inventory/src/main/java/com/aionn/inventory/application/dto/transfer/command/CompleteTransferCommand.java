package com.aionn.inventory.application.dto.transfer.command;

import com.aionn.sharedkernel.application.command.Command;

public record CompleteTransferCommand(
        String ownerId,
        String transferId,
        int receivedQty) implements Command {
}
