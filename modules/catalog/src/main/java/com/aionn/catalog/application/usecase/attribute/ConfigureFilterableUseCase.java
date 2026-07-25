package com.aionn.catalog.application.usecase.attribute;

import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.mapper.AttributeTemplateResultMapper;
import com.aionn.catalog.application.port.in.attribute.ConfigureFilterableInputPort;
import com.aionn.catalog.application.service.AttributeTemplateService;
import com.aionn.catalog.domain.model.AttributeTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfigureFilterableUseCase implements ConfigureFilterableInputPort {

    private final AttributeTemplateService attributeTemplateService;
    private final AttributeTemplateResultMapper attributeTemplateResultMapper;

    @Override
    @Transactional
    public AttributeTemplateResult execute(ConfigureFilterableCommand command) {
        AttributeTemplate template = attributeTemplateService.configureFilterable(command);
        return attributeTemplateResultMapper.toResult(template);
    }
}
