package com.aionn.catalog.adapter.rest.mapper.attribute;

import com.aionn.catalog.adapter.rest.dto.attribute.request.ConfigureFilterableRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.request.CreateAttributeTemplateRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.response.AttributeTemplateResponse;
import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateByCategoryQuery;
import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateQuery;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeDtoMapper {

    CreateAttributeTemplateCommand toCreateAttributeTemplateCommand(CreateAttributeTemplateRequest request);

    ConfigureFilterableCommand toConfigureFilterableCommand(String templateId, ConfigureFilterableRequest request);

    AttributeTemplateResponse toResponse(AttributeTemplateResult result);

    List<AttributeTemplateResponse> toResponses(List<AttributeTemplateResult> results);

    default GetAttributeTemplateQuery toGetQuery(String templateId) {
        return new GetAttributeTemplateQuery(templateId);
    }

    default GetAttributeTemplateByCategoryQuery toGetByCategoryQuery(String categoryId) {
        return new GetAttributeTemplateByCategoryQuery(categoryId);
    }
}
