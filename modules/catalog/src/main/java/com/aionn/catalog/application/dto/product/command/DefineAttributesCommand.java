package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.Map;

public record DefineAttributesCommand(
        String productId,
        String merchantId,
        Map<String, String> attributes) implements Command {
}
