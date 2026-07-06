package com.aionn.catalog.domain.event;

import java.time.Instant;
import java.util.List;

public final class AttributeTemplateEvents {

    private AttributeTemplateEvents() {
    }

    public record AttributeTemplateCreated(
            String templateId,
            String categoryId,
            List<String> attributes,
            Instant occurredAt) implements CatalogEvent {
    }

    public record FilterableAttrConfigured(
            String templateId,
            String attributeKey,
            boolean isFilterable,
            Instant occurredAt) implements CatalogEvent {
    }
}
