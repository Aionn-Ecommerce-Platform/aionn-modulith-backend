package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record AssignBrandCommand(String productId, String merchantId, String brandId) implements Command {
}
