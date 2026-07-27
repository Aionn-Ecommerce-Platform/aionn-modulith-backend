package com.aionn.notification.application.usecase.template;

import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.mapper.TemplateResultMapper;
import com.aionn.notification.application.port.in.template.UpdateTemplateInputPort;
import com.aionn.notification.application.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateTemplateUseCase implements UpdateTemplateInputPort {

    private final NotificationTemplateService templateService;
    private final TemplateResultMapper templateResultMapper;

    @Override
    @Transactional
    public TemplateResult execute(TemplateCommands.UpdateTemplate command) {
        return templateResultMapper.toResult(templateService.update(command));
    }
}