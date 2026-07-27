package com.aionn.notification.application.port.in.template;

import com.aionn.notification.application.dto.template.result.TemplateResult;

import java.util.List;

public interface ListTemplatesInputPort {
    List<TemplateResult> execute(int limit);
}