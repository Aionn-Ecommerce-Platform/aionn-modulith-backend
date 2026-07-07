package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.mapper.AttributeTemplateResultMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AttributeTemplateService {

    private final AttributeTemplatePersistencePort attributeTemplateRepository;
    private final CategoryPersistencePort categoryRepository;
    private final AttributeTemplateResultMapper attributeTemplateResultMapper;
    private final EventPublisher eventPublisher;

    public AttributeTemplateResult create(CreateAttributeTemplateCommand command) {
        categoryRepository.findById(command.categoryId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND));
        if (attributeTemplateRepository.findByCategoryId(command.categoryId()).isPresent()) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                    "Attribute template already exists for this category");
        }
        AttributeTemplate template = AttributeTemplate.create(IdGenerator.ulid(),
                command.categoryId(), command.attributeKeys());
        AttributeTemplate saved = attributeTemplateRepository.save(template);
        eventPublisher.publish(template.pullEvents());
        return attributeTemplateResultMapper.toResult(saved);
    }

    public AttributeTemplateResult configureFilterable(ConfigureFilterableCommand command) {
        AttributeTemplate template = attributeTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND));
        template.configureFilterable(command.attributeKey(), command.filterable());
        AttributeTemplate saved = attributeTemplateRepository.save(template);
        eventPublisher.publish(template.pullEvents());
        return attributeTemplateResultMapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public AttributeTemplateResult get(String templateId) {
        return attributeTemplateResultMapper.toResult(attributeTemplateRepository.findById(templateId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public AttributeTemplateResult getByCategory(String categoryId) {
        return attributeTemplateResultMapper.toResult(attributeTemplateRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND)));
    }
}
