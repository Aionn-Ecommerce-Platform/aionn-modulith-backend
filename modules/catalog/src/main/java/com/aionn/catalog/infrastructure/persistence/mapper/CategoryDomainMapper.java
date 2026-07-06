package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Category;
import com.aionn.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.aionn.catalog.infrastructure.persistence.entity.CategoryTranslationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryDomainMapper {

    @Mapping(target = "translations", ignore = true)
    CategoryEntity toEntityBasic(Category category);

    default CategoryEntity toEntity(Category category) {
        if (category == null) return null;
        CategoryEntity entity = toEntityBasic(category);

        List<CategoryTranslationEntity> translationEntities = new ArrayList<>();
        if (category.translations() != null) {
            for (Category.Translation trans : category.translations()) {
                CategoryTranslationEntity te = CategoryTranslationEntity.builder()
                        .id(new CategoryTranslationEntity.CategoryTranslationId(category.getCategoryId(), trans.locale()))
                        .category(entity)
                        .name(trans.name())
                        .build();
                translationEntities.add(te);
            }
        }
        entity.setTranslations(translationEntities);
        return entity;
    }

    @Mapping(target = "translations", ignore = true)
    Category toDomainBasic(CategoryEntity entity);

    default Category toDomain(CategoryEntity entity) {
        if (entity == null) return null;
        Category category = toDomainBasic(entity);
        
        List<Category.Translation> translations = new ArrayList<>();
        if (entity.getTranslations() != null) {
            for (CategoryTranslationEntity te : entity.getTranslations()) {
                translations.add(new Category.Translation(te.getId().getLocale(), te.getName()));
            }
        }
        return new Category(
                category.getCategoryId(),
                category.getParentId(),
                category.getName(),
                category.getSlug(),
                category.getIconUrl(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getDeletedAt(),
                translations);
    }
}

