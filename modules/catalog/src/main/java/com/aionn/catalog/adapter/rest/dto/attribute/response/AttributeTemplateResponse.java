package com.aionn.catalog.adapter.rest.dto.attribute.response;

import java.time.Instant;
import java.util.Map;

public record AttributeTemplateResponse(
        String templateId,
        String categoryId,
        Map<String, Boolean> attributes,
        Instant createdAt,
        Instant updatedAt) {
}
