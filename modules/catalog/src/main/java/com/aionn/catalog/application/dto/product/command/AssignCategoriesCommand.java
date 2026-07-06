package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record AssignCategoriesCommand(
        String productId,
        String merchantId,
        List<String> categoryIds) implements Command {
}
