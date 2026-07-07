package com.aionn.catalog.application.dto.brand.command;

import com.aionn.sharedkernel.application.command.Command;

public record CreateBrandCommand(String name, String logoUrl, String description) implements Command {
}
