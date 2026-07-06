package com.aionn.catalog.application.usecase.attribute;

import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.port.in.attribute.CreateAttributeTemplateInputPort;
import com.aionn.catalog.application.service.AttributeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAttributeTemplateUseCase implements CreateAttributeTemplateInputPort {

    private final AttributeTemplateService attributeTemplateService;

    @Override
    public AttributeTemplateResult execute(CreateAttributeTemplateCommand command) {
        return attributeTemplateService.create(command);
    }
}
