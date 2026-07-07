package com.aionn.catalog.application.usecase.attribute;

import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.port.in.attribute.ConfigureFilterableInputPort;
import com.aionn.catalog.application.service.AttributeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigureFilterableUseCase implements ConfigureFilterableInputPort {

    private final AttributeTemplateService attributeTemplateService;

    @Override
    public AttributeTemplateResult execute(ConfigureFilterableCommand command) {
        return attributeTemplateService.configureFilterable(command);
    }
}
