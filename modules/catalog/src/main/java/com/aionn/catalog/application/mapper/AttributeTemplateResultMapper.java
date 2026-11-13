package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.domain.model.AttributeTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttributeTemplateResultMapper {

    @Mapping(target = "attributes", expression = "java(template.filterabilityMap())")
    AttributeTemplateResult toResult(AttributeTemplate template);
}
