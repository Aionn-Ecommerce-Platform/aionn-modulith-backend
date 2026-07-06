package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record PublishProductCommand(String productId, String adminId) implements Command {
}
