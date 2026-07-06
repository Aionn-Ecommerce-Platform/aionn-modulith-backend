package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.infrastructure.persistence.entity.AttributeTemplateEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AttributeTemplateDomainMapper {

    public AttributeTemplateEntity toEntity(AttributeTemplate template) {
        if (template == null) {
            return null;
        }
        return AttributeTemplateEntity.builder()
                .templateId(template.getTemplateId())
                .categoryId(template.getCategoryId())
                .attributes(template.filterabilityMap())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    public AttributeTemplate toDomain(AttributeTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, AttributeTemplate.AttributeDefinition> defs = new LinkedHashMap<>();
        if (entity.getAttributes() != null) {
            entity.getAttributes().forEach((k, v) -> defs.put(k, new AttributeTemplate.AttributeDefinition(k, v)));
        }
        return new AttributeTemplate(
                entity.getTemplateId(),
                entity.getCategoryId(),
                defs,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
