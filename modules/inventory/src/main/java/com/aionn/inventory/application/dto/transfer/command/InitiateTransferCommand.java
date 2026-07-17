package com.aionn.inventory.application.dto.transfer.command;

import com.aionn.sharedkernel.application.command.Command;

public record InitiateTransferCommand(
        String ownerId,
        String fromWarehouseId,
        String toWarehouseId,
        String skuId,
        int qty) implements Command {
}
