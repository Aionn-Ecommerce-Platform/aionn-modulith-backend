package com.aionn.inventory.application.dto.reservation.command;

import com.aionn.sharedkernel.application.command.Command;

public record ReserveStockCommand(
        String skuId,
        String warehouseId,
        String orderId,
        int qty,
        int ttlSeconds) implements Command {
}
