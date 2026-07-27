package com.aionn.notification.application.dto.template.result;

import java.time.Instant;
import java.util.List;

public record TemplateResult(
        String templateId,
        String eventType,
        String channel,
        String category,
        String locale,
        String subject,
        String content,
        List<String> placeholders,
        int version,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}

