package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record DeactivateProductCommand(String productId, String merchantId, String reason) implements Command {
}
