package com.aionn.catalog.application.dto.attribute.result;

import java.time.Instant;
import java.util.Map;

public record AttributeTemplateResult(
        String templateId,
        String categoryId,
        Map<String, Boolean> attributes,
        Instant createdAt,
        Instant updatedAt) {
}

