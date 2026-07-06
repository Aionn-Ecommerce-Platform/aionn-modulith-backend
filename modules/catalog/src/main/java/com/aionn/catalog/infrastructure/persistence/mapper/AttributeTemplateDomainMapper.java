package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.infrastructure.persistence.entity.AttributeTemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttributeTemplateDomainMapper {

    @Mapping(target = "attributes", ignore = true)
    AttributeTemplateEntity toEntityBasic(AttributeTemplate template);
    
    default AttributeTemplateEntity toEntity(AttributeTemplate template) {
        if (template == null) return null;
        AttributeTemplateEntity entity = toEntityBasic(template);
        Map<String, Boolean> attrs = new LinkedHashMap<>();
        if (template.snapshot() != null) {
            template.snapshot().forEach((k, v) -> attrs.put(k, v.filterable()));
        }
        entity.setAttributes(attrs);
        return entity;
    }
    
    @Mapping(target = "attributes", ignore = true)
    AttributeTemplate toDomainBasic(AttributeTemplateEntity entity);
    
    default AttributeTemplate toDomain(AttributeTemplateEntity entity) {
        if (entity == null) return null;
        AttributeTemplate domain = toDomainBasic(entity);
        Map<String, AttributeTemplate.AttributeDefinition> defs = new LinkedHashMap<>();
        if (entity.getAttributes() != null) {
            entity.getAttributes().forEach((k, v) -> defs.put(k, new AttributeTemplate.AttributeDefinition(k, v)));
        }
        return new AttributeTemplate(domain.getTemplateId(), domain.getCategoryId(), defs, domain.getCreatedAt(), domain.getUpdatedAt());
    }
}

