package com.aionn.catalog.infrastructure.persistence.repository.attribute;

import com.aionn.catalog.infrastructure.persistence.entity.AttributeTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttributeTemplateRepository extends JpaRepository<AttributeTemplateEntity, String> {

    Optional<AttributeTemplateEntity> findByCategoryId(String categoryId);

    List<AttributeTemplateEntity> findByCategoryIdIn(Collection<String> categoryIds);
}
