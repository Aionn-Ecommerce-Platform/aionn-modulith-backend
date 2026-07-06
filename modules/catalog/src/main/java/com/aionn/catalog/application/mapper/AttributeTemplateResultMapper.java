package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.domain.model.AttributeTemplate;
import org.springframework.stereotype.Component;

@Component
public class AttributeTemplateResultMapper {

    public AttributeTemplateResult toResult(AttributeTemplate template) {
        return new AttributeTemplateResult(
                template.getTemplateId(),
                template.getCategoryId(),
                template.filterabilityMap(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
