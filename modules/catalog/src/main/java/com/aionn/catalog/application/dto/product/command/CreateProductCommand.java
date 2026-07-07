package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record CreateProductCommand(String merchantId, String name) implements Command {
}
