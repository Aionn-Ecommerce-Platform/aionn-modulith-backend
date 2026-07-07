package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.catalog.infrastructure.persistence.entity.ProductEntity;
import com.aionn.catalog.infrastructure.persistence.entity.ProductTranslationEntity;
import com.aionn.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.aionn.sharedkernel.domain.vo.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductDomainMapper {

    @Mapping(target = "status", expression = "java(product.getStatus() != null ? product.getStatus().name() : null)")
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "translations", ignore = true)
    ProductEntity toEntityBasic(Product product);

    default ProductEntity toEntity(Product product) {
        if (product == null) return null;
        ProductEntity entity = toEntityBasic(product);

        List<ProductVariantEntity> variantEntities = new ArrayList<>();
        if (product.variants() != null) {
            for (ProductVariant variant : product.variants()) {
                ProductVariantEntity ve = ProductVariantEntity.builder()
                        .skuId(variant.skuId())
                        .product(entity)
                        .attributeValues(variant.attributeValues() != null ? new HashMap<>(variant.attributeValues()) : new HashMap<>())
                        .price(Optional.ofNullable(variant.price()).map(Money::amount).orElse(null))
                        .originalPrice(Optional.ofNullable(variant.originalPrice()).map(Money::amount).orElse(null))
                        .currency(Optional.ofNullable(variant.price()).map(Money::currency).orElse(null))
                        .build();
                variantEntities.add(ve);
            }
        }
        entity.setVariants(variantEntities);

        List<ProductTranslationEntity> translationEntities = new ArrayList<>();
        if (product.translations() != null) {
            for (Product.Translation trans : product.translations()) {
                ProductTranslationEntity te = ProductTranslationEntity.builder()
                        .id(new ProductTranslationEntity.ProductTranslationId(product.getProductId(), trans.locale()))
                        .product(entity)
                        .name(trans.name())
                        .aiDescription(trans.aiDescription())
                        .build();
                translationEntities.add(te);
            }
        }
        entity.setTranslations(translationEntities);

        return entity;
    }

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "translations", ignore = true)
    Product toDomainBasic(ProductEntity entity);

    default Product toDomain(ProductEntity entity) {
        if (entity == null) return null;
        Product base = toDomainBasic(entity);

        List<ProductVariant> variants = new ArrayList<>();
        if (entity.getVariants() != null) {
            for (ProductVariantEntity ve : entity.getVariants()) {
                Money price = ve.getPrice() != null && ve.getCurrency() != null
                        ? Money.of(ve.getPrice(), ve.getCurrency())
                        : null;
                Money originalPrice = ve.getOriginalPrice() != null && ve.getCurrency() != null
                        ? Money.of(ve.getOriginalPrice(), ve.getCurrency())
                        : null;
                variants.add(new ProductVariant(ve.getSkuId(),
                        ve.getAttributeValues() != null ? ve.getAttributeValues() : Map.of(),
                        price,
                        originalPrice));
            }
        }

        List<Product.Translation> translations = new ArrayList<>();
        if (entity.getTranslations() != null) {
            for (ProductTranslationEntity te : entity.getTranslations()) {
                translations.add(new Product.Translation(te.getId().getLocale(), te.getName(), te.getAiDescription()));
            }
        }

        return new Product(
                base.getProductId(),
                base.getMerchantId(),
                base.getName(),
                base.getBrandId(),
                base.categoryIds() != null ? base.categoryIds() : List.of(),
                base.imageList() != null ? base.imageList() : List.of(),
                base.tags() != null ? base.tags() : List.of(),
                base.collectionIds() != null ? base.collectionIds() : List.of(),
                base.attributes() != null ? base.attributes() : Map.of(),
                variants,
                base.getAiDescription(),
                entity.getStatus() != null ? ProductStatus.valueOf(entity.getStatus()) : null,
                base.getCreatedAt(),
                base.getUpdatedAt(),
                translations);
    }
}

