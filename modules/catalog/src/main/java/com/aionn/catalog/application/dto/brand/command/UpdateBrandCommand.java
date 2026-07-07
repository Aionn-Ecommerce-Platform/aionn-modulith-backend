package com.aionn.catalog.application.dto.brand.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateBrandCommand(String brandId, String name, String logoUrl, String description) implements Command {
}
