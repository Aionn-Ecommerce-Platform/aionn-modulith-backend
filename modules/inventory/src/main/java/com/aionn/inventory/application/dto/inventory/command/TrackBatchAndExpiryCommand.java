package com.aionn.inventory.application.dto.inventory.command;

import com.aionn.sharedkernel.application.command.Command;

import java.time.LocalDate;

public record TrackBatchAndExpiryCommand(
        String ownerId,
        String skuId,
        String warehouseId,
        String batchNo,
        LocalDate expiryDate) implements Command {
}
