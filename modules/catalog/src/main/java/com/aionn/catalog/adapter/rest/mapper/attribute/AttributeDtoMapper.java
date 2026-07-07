package com.aionn.catalog.adapter.rest.mapper.attribute;

import com.aionn.catalog.adapter.rest.dto.attribute.ConfigureFilterableRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.CreateAttributeTemplateRequest;
import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttributeDtoMapper {

    CreateAttributeTemplateCommand toCreateAttributeTemplateCommand(CreateAttributeTemplateRequest request);

    ConfigureFilterableCommand toConfigureFilterableCommand(String templateId, ConfigureFilterableRequest request);
}
