package com.aionn.notification.application.port.in.template;

import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;

public interface UpdateTemplateInputPort {
    TemplateResult execute(TemplateCommands.UpdateTemplate command);
}