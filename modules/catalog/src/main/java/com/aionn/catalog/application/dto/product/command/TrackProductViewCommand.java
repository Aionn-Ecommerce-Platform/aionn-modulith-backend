package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

public record TrackProductViewCommand(String productId, String userId) implements Command {
}
