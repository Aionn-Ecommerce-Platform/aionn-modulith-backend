package com.aionn.catalog.application.dto.category.command;

import com.aionn.sharedkernel.application.command.Command;

public record MoveCategoryCommand(String categoryId, String newParentId) implements Command {
}
