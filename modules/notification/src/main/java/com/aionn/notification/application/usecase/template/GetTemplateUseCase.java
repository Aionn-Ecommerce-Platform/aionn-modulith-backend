package com.aionn.notification.application.usecase.template;

import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.mapper.TemplateResultMapper;
import com.aionn.notification.application.port.in.template.GetTemplateInputPort;
import com.aionn.notification.application.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTemplateUseCase implements GetTemplateInputPort {

    private final NotificationTemplateService templateService;
    private final TemplateResultMapper templateResultMapper;

    @Override
    @Transactional(readOnly = true)
    public TemplateResult execute(String templateId) {
        return templateResultMapper.toResult(templateService.get(templateId));
    }
}