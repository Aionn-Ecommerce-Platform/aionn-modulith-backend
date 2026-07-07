package com.aionn.catalog.application.port.in.attribute;

import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;

public interface ConfigureFilterableInputPort {

    AttributeTemplateResult execute(ConfigureFilterableCommand command);
}
