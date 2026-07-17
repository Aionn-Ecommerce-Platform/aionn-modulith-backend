package com.aionn.catalog.adapter.rest.mapper.attribute;

import com.aionn.catalog.adapter.rest.dto.attribute.request.ConfigureFilterableRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.request.CreateAttributeTemplateRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.response.AttributeTemplateResponse;
import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeDtoMapper {

    CreateAttributeTemplateCommand toCreateAttributeTemplateCommand(CreateAttributeTemplateRequest request);

    ConfigureFilterableCommand toConfigureFilterableCommand(String templateId, ConfigureFilterableRequest request);

    AttributeTemplateResponse toResponse(AttributeTemplateResult result);

    List<AttributeTemplateResponse> toResponses(List<AttributeTemplateResult> results);
}
