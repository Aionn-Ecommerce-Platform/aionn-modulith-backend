package com.aionn.catalog.application.dto.category.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateCategoryCommand(
                String categoryId,
                String name,
                String iconUrl,
                Boolean active) implements Command {
}
