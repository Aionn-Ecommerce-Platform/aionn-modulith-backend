package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record UpdateMediaCommand(String productId, String merchantId, List<String> images) implements Command {
}
