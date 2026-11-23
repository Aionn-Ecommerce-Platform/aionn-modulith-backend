package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateDescriptionCommand(
        String productId,
        String merchantId,
        String description) implements Command {
}
