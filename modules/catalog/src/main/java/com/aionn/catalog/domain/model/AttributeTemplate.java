package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.catalog.domain.event.AttributeTemplateEvents;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class AttributeTemplate extends AggregateRoot {

        private final String templateId;
        private final String categoryId;
        @Getter(AccessLevel.NONE)
        private final Map<String, AttributeDefinition> attributes;
        private final Instant createdAt;
        private Instant updatedAt;

        public AttributeTemplate(
                        String templateId,
                        String categoryId,
                        Map<String, AttributeDefinition> attributes,
                        Instant createdAt,
                        Instant updatedAt) {
                this.templateId = templateId;
                this.categoryId = categoryId;
                this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
        }

        public static AttributeTemplate create(String templateId, String categoryId, List<String> attributeKeys) {
                return create(templateId, categoryId, attributeKeys, Clock.systemUTC());
        }

        public static AttributeTemplate create(String templateId, String categoryId, List<String> attributeKeys, Clock clock) {
                Guard.require(templateId != null && !templateId.isBlank(),
                                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                                                "templateId must not be blank"));
                Guard.require(categoryId != null && !categoryId.isBlank(),
                                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                                                "categoryId must not be blank"));
                Guard.require(attributeKeys != null && !attributeKeys.isEmpty(),
                                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                                                "attributes must not be empty"));
                Map<String, AttributeDefinition> initial = new LinkedHashMap<>();
                List<String> trimmedKeys = new java.util.ArrayList<>();
                for (String key : attributeKeys) {
                        Guard.require(key != null && !key.isBlank(),
                                        () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                                                        "attribute key must not be blank"));
                        String trimmed = key.trim();
                        Guard.require(!initial.containsKey(trimmed),
                                        () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                                                        "attribute key must be unique: " + trimmed));
                        initial.put(trimmed, new AttributeDefinition(trimmed, true));
                        trimmedKeys.add(trimmed);
                }
                Instant now = clock.instant();
                AttributeTemplate template = new AttributeTemplate(templateId, categoryId, initial, now, now);
                template.registerEvent(new AttributeTemplateEvents.AttributeTemplateCreated(
                                templateId, categoryId, List.copyOf(trimmedKeys), now));
                return template;
        }

        public void configureFilterable(String attributeKey, boolean filterable) {
                configureFilterable(attributeKey, filterable, Clock.systemUTC());
        }

        public void configureFilterable(String attributeKey, boolean filterable, Clock clock) {
                Guard.require(attributeKey != null && !attributeKey.isBlank(),
                                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                                                "attributeKey must not be blank"));
                String trimmed = attributeKey.trim();
                Guard.require(attributes.containsKey(trimmed),
                                () -> new CatalogException(CatalogErrorCode.ATTRIBUTE_KEY_NOT_FOUND,
                                                "Attribute not declared: " + trimmed));
                AttributeDefinition current = attributes.get(trimmed);
                if (current.filterable() == filterable) {
                        return;
                }
                attributes.put(trimmed, new AttributeDefinition(trimmed, filterable));
                this.updatedAt = clock.instant();
                registerEvent(new AttributeTemplateEvents.FilterableAttrConfigured(
                                templateId, trimmed, filterable, updatedAt));
        }

        public Map<String, AttributeDefinition> snapshot() {
                return Map.copyOf(attributes);
        }

        public Map<String, Boolean> filterabilityMap() {
                Map<String, Boolean> map = new LinkedHashMap<>();
                attributes.forEach((k, v) -> map.put(k, v.filterable()));
                return Map.copyOf(map);
        }

        public record AttributeDefinition(String key, boolean filterable) {
        }

        @Override
        protected String aggregateId() {
                return templateId;
        }
}
