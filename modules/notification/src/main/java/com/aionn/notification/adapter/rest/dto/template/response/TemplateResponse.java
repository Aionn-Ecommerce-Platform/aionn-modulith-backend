package com.aionn.notification.adapter.rest.dto.template.response;

import java.time.Instant;
import java.util.List;

public record TemplateResponse(
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
