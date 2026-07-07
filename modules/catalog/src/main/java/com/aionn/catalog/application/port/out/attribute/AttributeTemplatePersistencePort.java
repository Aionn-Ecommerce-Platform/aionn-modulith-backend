package com.aionn.catalog.application.port.out.attribute;

import com.aionn.catalog.domain.model.AttributeTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttributeTemplatePersistencePort {

    AttributeTemplate save(AttributeTemplate template);

    Optional<AttributeTemplate> findById(String templateId);

    Optional<AttributeTemplate> findByCategoryId(String categoryId);

    List<AttributeTemplate> findByCategoryIds(Collection<String> categoryIds);
}
