package com.aionn.catalog.application.dto.category.command;

import com.aionn.sharedkernel.application.command.Command;

public record DeleteCategoryCommand(String categoryId) implements Command {
}
