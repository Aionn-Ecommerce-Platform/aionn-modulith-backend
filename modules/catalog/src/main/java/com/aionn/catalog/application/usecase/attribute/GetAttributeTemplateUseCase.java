package com.aionn.catalog.application.usecase.attribute;

import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateQuery;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.port.in.attribute.GetAttributeTemplateInputPort;
import com.aionn.catalog.application.service.AttributeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAttributeTemplateUseCase implements GetAttributeTemplateInputPort {

    private final AttributeTemplateService attributeTemplateService;

    @Override
    public AttributeTemplateResult execute(GetAttributeTemplateQuery query) {
        return attributeTemplateService.get(query.templateId());
    }
}
