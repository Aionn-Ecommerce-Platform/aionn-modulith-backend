package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.contracts.port.catalog.CatalogProductAttributeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exposes product classification to other modules without leaking catalog's
 * domain types. Only
 * published products surface: recommending an unpublished or taken-down product
 * would let a user
 * follow a link to something they cannot buy.
 */
@Component
@RequiredArgsConstructor
public class CatalogProductAttributeQueryAdapter implements CatalogProductAttributeQueryPort {

    private final ProductPersistencePort productPersistencePort;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductAttributeView> findByProductId(String productId) {
        return productPersistencePort.findById(productId)
                .filter(CatalogProductAttributeQueryAdapter::isPublished)
                .map(productResultMapper::toResult)
                .map(CatalogProductAttributeQueryAdapter::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, ProductAttributeView> findByProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productPersistencePort.findAllByIds(productIds).stream()
                .filter(CatalogProductAttributeQueryAdapter::isPublished)
                .toList();
        Map<String, ProductAttributeView> views = new LinkedHashMap<>();
        for (ProductAttributeView view : toViews(productResultMapper.toResults(products))) {
            views.put(view.productId(), view);
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findProductIdBySkuId(String skuId) {
        if (skuId == null || skuId.isBlank()) {
            return Optional.empty();
        }
        return productPersistencePort.findAllBySkuIds(List.of(skuId)).stream()
                .filter(CatalogProductAttributeQueryAdapter::isPublished)
                .map(Product::getProductId)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> productIdBySku = new LinkedHashMap<>();
        for (Product product : productPersistencePort.findAllBySkuIds(skuIds)) {
            if (!isPublished(product)) {
                continue;
            }
            for (ProductVariant variant : product.variants()) {
                if (skuIds.contains(variant.skuId())) {
                    productIdBySku.put(variant.skuId(), product.getProductId());
                }
            }
        }
        return productIdBySku;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeView> findByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit) {
        if ((categoryIds == null || categoryIds.isEmpty()) && (brandIds == null || brandIds.isEmpty())) {
            return List.of();
        }
        List<Product> products = productPersistencePort.findPersonalizedProducts(
                categoryIds == null ? List.of() : List.copyOf(categoryIds),
                brandIds == null ? List.of() : List.copyOf(brandIds),
                Math.max(1, limit));
        return toViews(productResultMapper.toResults(products.stream()
                .filter(CatalogProductAttributeQueryAdapter::isPublished)
                .toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeView> findRecentlyPublished(int limit) {
        return toViews(productResultMapper.toResults(productPersistencePort.findPublished(Math.max(1, limit), 0).stream()
                .filter(CatalogProductAttributeQueryAdapter::isPublished)
                .toList()));
    }

    private static List<ProductAttributeView> toViews(List<ProductResult> results) {
        List<ProductAttributeView> views = new ArrayList<>(results.size());
        for (ProductResult result : results) {
            views.add(toView(result));
        }
        return views;
    }

    private static boolean isPublished(Product product) {
        return product != null && product.getStatus() == ProductStatus.PUBLISHED;
    }

    private static ProductAttributeView toView(ProductResult result) {
        List<ProductResult.Variant> variants = result.variants();
        List<String> skuIds = new ArrayList<>(variants.size());
        BigDecimal cheapest = null;
        BigDecimal cheapestOriginal = null;
        String currency = null;
        for (ProductResult.Variant variant : variants) {
            skuIds.add(variant.skuId());
            if (variant.price() == null) {
                continue;
            }
            if (cheapest == null) {
                cheapest = variant.price();
                cheapestOriginal = variant.originalPrice();
                currency = variant.currency();
            } else if (currency != null && currency.equalsIgnoreCase(variant.currency())
                    && variant.price().compareTo(cheapest) < 0) {
                cheapest = variant.price();
                cheapestOriginal = variant.originalPrice();
            }
        }
        List<String> images = result.imageList();
        return new ProductAttributeView(
                result.productId(),
                result.name(),
                result.brandId(),
                result.categoryIds(),
                skuIds,
                images == null || images.isEmpty() ? null : images.get(0),
                cheapest,
                cheapestOriginal,
                currency,
                result.rating(),
                result.reviewCount(),
                result.soldCount());
    }
}
