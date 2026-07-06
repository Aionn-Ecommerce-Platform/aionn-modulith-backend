package com.aionn.catalog.application.dto.attribute.command;

import com.aionn.sharedkernel.application.command.Command;

public record ConfigureFilterableCommand(String templateId, String attributeKey, boolean filterable)
        implements Command {
}
