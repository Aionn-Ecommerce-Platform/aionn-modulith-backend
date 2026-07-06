package com.aionn.catalog.application.dto.attribute.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record CreateAttributeTemplateCommand(String categoryId, List<String> attributeKeys) implements Command {
}
