package com.aionn.catalog.application.dto.category.command;

import com.aionn.sharedkernel.application.command.Command;

public record CreateCategoryCommand(String parentId, String name, String slug) implements Command {
}
