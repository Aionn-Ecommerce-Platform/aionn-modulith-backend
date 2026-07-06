package com.aionn.catalog.application.dto.brand.command;

import com.aionn.sharedkernel.application.command.Command;

public record DeleteBrandCommand(String brandId, String reason) implements Command {
}
