package com.aionn.notification.application.usecase.template;

import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.mapper.TemplateResultMapper;
import com.aionn.notification.application.port.in.template.ListTemplatesInputPort;
import com.aionn.notification.application.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListTemplatesUseCase implements ListTemplatesInputPort {

    private final NotificationTemplateService templateService;
    private final TemplateResultMapper templateResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResult> execute(int limit) {
        return templateResultMapper.toResults(templateService.list(limit));
    }
}