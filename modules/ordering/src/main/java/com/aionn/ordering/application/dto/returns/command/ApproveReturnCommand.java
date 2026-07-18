package com.aionn.ordering.application.dto.returns.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ApproveReturnCommand(
        String returnId,
        String ownerId,
        BigDecimal refundAmount,
        String currency,
        String returnWarehouseId) implements Command {
}
