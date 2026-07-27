package com.aionn.notification.adapter.rest.mapper.template;

import com.aionn.notification.adapter.rest.dto.template.CreateTemplateRequest;
import com.aionn.notification.adapter.rest.dto.template.UpdateTemplateRequest;
import com.aionn.notification.adapter.rest.dto.template.response.TemplateResponse;
import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationTemplateDtoMapper {

    TemplateResponse toResponse(TemplateResult result);

    List<TemplateResponse> toResponses(List<TemplateResult> results);

    default TemplateCommands.CreateTemplate toCreateCommand(CreateTemplateRequest request) {
        return new TemplateCommands.CreateTemplate(request.eventType(), request.channel(),
                request.category(), request.locale(), request.subject(), request.content());
    }

    default TemplateCommands.UpdateTemplate toUpdateCommand(String templateId,
            UpdateTemplateRequest request) {
        return new TemplateCommands.UpdateTemplate(templateId, request.subject(), request.content());
    }
}
