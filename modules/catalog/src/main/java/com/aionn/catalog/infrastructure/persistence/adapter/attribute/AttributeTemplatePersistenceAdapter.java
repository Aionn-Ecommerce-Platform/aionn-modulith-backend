package com.aionn.catalog.infrastructure.persistence.adapter.attribute;

import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.infrastructure.persistence.mapper.AttributeTemplateDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.attribute.AttributeTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AttributeTemplatePersistenceAdapter implements AttributeTemplatePersistencePort {

    private final AttributeTemplateRepository jpa;
    private final AttributeTemplateDomainMapper mapper;

    @Override
    public AttributeTemplate save(AttributeTemplate template) {
        try {
            return mapper.toDomain(jpa.save(mapper.toEntity(template)));
        } catch (DataIntegrityViolationException e) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                    "Attribute template already exists for this category");
        }
    }

    @Override
    public Optional<AttributeTemplate> findById(String templateId) {
        return jpa.findById(templateId).map(mapper::toDomain);
    }

    @Override
    public Optional<AttributeTemplate> findByCategoryId(String categoryId) {
        return jpa.findByCategoryId(categoryId).map(mapper::toDomain);
    }

    @Override
    public List<AttributeTemplate> findByCategoryIds(Collection<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByCategoryIdIn(categoryIds).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
