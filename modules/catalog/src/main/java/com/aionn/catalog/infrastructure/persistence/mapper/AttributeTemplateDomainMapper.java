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

    @Mapping(target = "attributes", expression = "java(template.filterabilityMap())")
    AttributeTemplateEntity toEntity(AttributeTemplate template);

    default AttributeTemplate toDomain(AttributeTemplateEntity entity) {
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
