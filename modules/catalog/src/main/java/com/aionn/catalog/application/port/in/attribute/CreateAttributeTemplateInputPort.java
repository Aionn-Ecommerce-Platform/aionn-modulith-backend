package com.aionn.catalog.application.port.in.attribute;

import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;

public interface CreateAttributeTemplateInputPort {

    AttributeTemplateResult execute(CreateAttributeTemplateCommand command);
}
