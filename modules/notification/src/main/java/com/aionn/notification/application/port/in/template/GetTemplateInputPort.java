package com.aionn.notification.application.port.in.template;

import com.aionn.notification.application.dto.template.result.TemplateResult;

public interface GetTemplateInputPort {
    TemplateResult execute(String templateId);
}