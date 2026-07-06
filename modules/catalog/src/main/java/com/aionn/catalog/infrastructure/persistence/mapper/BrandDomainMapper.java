package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.infrastructure.persistence.entity.BrandEntity;
import com.aionn.catalog.infrastructure.persistence.entity.BrandTranslationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BrandDomainMapper {

    @Mapping(target = "status", expression = "java(brand.getStatus() != null ? brand.getStatus().name() : null)")
    @Mapping(target = "translations", ignore = true)
    BrandEntity toEntityBasic(Brand brand);

    default BrandEntity toEntity(Brand brand) {
        if (brand == null) return null;
        BrandEntity entity = toEntityBasic(brand);

        List<BrandTranslationEntity> translationEntities = new ArrayList<>();
        if (brand.translations() != null) {
            for (Brand.Translation trans : brand.translations()) {
                BrandTranslationEntity te = BrandTranslationEntity.builder()
                        .id(new BrandTranslationEntity.BrandTranslationId(brand.getBrandId(), trans.locale()))
                        .brand(entity)
                        .name(trans.name())
                        .description(trans.description())
                        .build();
                translationEntities.add(te);
            }
        }
        entity.setTranslations(translationEntities);
        return entity;
    }

    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? com.aionn.catalog.domain.valueobject.BrandStatus.valueOf(entity.getStatus()) : null)")
    @Mapping(target = "translations", ignore = true)
    Brand toDomainBasic(BrandEntity entity);

    default Brand toDomain(BrandEntity entity) {
        if (entity == null) return null;
        Brand brand = toDomainBasic(entity);
        
        List<Brand.Translation> translations = new ArrayList<>();
        if (entity.getTranslations() != null) {
            for (BrandTranslationEntity te : entity.getTranslations()) {
                translations.add(new Brand.Translation(te.getId().getLocale(), te.getName(), te.getDescription()));
            }
        }
        return new Brand(
                brand.getBrandId(),
                brand.getName(),
                brand.getLogoUrl(),
                brand.getDescription(),
                brand.getStatus(),
                brand.getCreatedAt(),
                brand.getUpdatedAt(),
                translations);
    }
}

