package com.aionn.notification.application.mapper;

import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.domain.model.NotificationTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TemplateResultMapper {

    @Mapping(target = "channel", expression = "java(template.getChannel().name())")
    @Mapping(target = "category", expression = "java(template.getCategory().name())")
    @Mapping(target = "placeholders", expression = "java(java.util.List.copyOf(template.getPlaceholders()))")
    TemplateResult toResult(NotificationTemplate template);
}
