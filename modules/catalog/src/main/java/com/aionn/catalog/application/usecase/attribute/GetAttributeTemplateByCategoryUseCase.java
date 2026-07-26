package com.aionn.catalog.application.usecase.attribute;

import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateByCategoryQuery;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.mapper.AttributeTemplateResultMapper;
import com.aionn.catalog.application.port.in.attribute.GetAttributeTemplateByCategoryInputPort;
import com.aionn.catalog.application.service.AttributeTemplateService;
import com.aionn.catalog.domain.model.AttributeTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAttributeTemplateByCategoryUseCase implements GetAttributeTemplateByCategoryInputPort {

    private final AttributeTemplateService attributeTemplateService;
    private final AttributeTemplateResultMapper attributeTemplateResultMapper;

    @Override
    @Transactional(readOnly = true)
    public AttributeTemplateResult execute(GetAttributeTemplateByCategoryQuery query) {
        AttributeTemplate template = attributeTemplateService.getByCategory(query.categoryId());
        return attributeTemplateResultMapper.toResult(template);
    }
}
