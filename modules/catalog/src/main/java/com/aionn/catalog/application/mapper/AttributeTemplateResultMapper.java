package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.domain.model.AttributeTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AttributeTemplateResultMapper {

    public AttributeTemplateResult toResult(AttributeTemplate template) {
        Map<String, Boolean> attrs = new LinkedHashMap<>();
        template.snapshot().forEach((k, v) -> attrs.put(k, v.filterable()));
        return new AttributeTemplateResult(
                template.getTemplateId(),
                template.getCategoryId(),
                attrs,
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
