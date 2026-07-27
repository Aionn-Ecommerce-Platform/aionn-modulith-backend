package com.aionn.notification.application.usecase.template;

import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.mapper.TemplateResultMapper;
import com.aionn.notification.application.port.in.template.CreateTemplateInputPort;
import com.aionn.notification.application.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTemplateUseCase implements CreateTemplateInputPort {

    private final NotificationTemplateService templateService;
    private final TemplateResultMapper templateResultMapper;

    @Override
    @Transactional
    public TemplateResult execute(TemplateCommands.CreateTemplate command) {
        return templateResultMapper.toResult(templateService.create(command));
    }
}