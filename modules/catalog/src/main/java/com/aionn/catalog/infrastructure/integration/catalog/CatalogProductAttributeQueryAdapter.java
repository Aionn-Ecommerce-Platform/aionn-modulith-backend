package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.CatalogProductAttributeQueryPort;
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

    private final ProductPersistencePort productRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductAttributeView> findByProductId(String productId) {
        return productRepository.findById(productId)
                .filter(CatalogProductAttributeQueryAdapter::isPublished)
                .map(CatalogProductAttributeQueryAdapter::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, ProductAttributeView> findByProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ProductAttributeView> views = new LinkedHashMap<>();
        for (Product product : productRepository.findAllByIds(productIds)) {
            if (isPublished(product)) {
                views.put(product.getProductId(), toView(product));
            }
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findProductIdBySkuId(String skuId) {
        if (skuId == null || skuId.isBlank()) {
            return Optional.empty();
        }
        return productRepository.findAllBySkuIds(List.of(skuId)).stream()
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
        for (Product product : productRepository.findAllBySkuIds(skuIds)) {
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
        List<Product> products = productRepository.findPersonalizedProducts(
                categoryIds == null ? List.of() : List.copyOf(categoryIds),
                brandIds == null ? List.of() : List.copyOf(brandIds),
                Math.max(1, limit));
        return toViews(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeView> findRecentlyPublished(int limit) {
        return toViews(productRepository.findPublished(Math.max(1, limit), 0));
    }

    private static List<ProductAttributeView> toViews(List<Product> products) {
        List<ProductAttributeView> views = new ArrayList<>(products.size());
        for (Product product : products) {
            if (isPublished(product)) {
                views.add(toView(product));
            }
        }
        return views;
    }

    private static boolean isPublished(Product product) {
        return product != null && product.getStatus() == ProductStatus.PUBLISHED;
    }

    private static ProductAttributeView toView(Product product) {
        List<ProductVariant> variants = product.variants();
        List<String> skuIds = new ArrayList<>(variants.size());
        BigDecimal cheapest = null;
        String currency = null;
        for (ProductVariant variant : variants) {
            skuIds.add(variant.skuId());
            Money price = variant.price();
            if (price == null) {
                continue;
            }
            if (cheapest == null) {
                cheapest = price.amount();
                currency = price.currency();
            } else if (currency != null && currency.equalsIgnoreCase(price.currency())
                    && price.amount().compareTo(cheapest) < 0) {
                cheapest = price.amount();
            }
        }
        List<String> images = product.imageList();
        return new ProductAttributeView(
                product.getProductId(),
                product.getName(),
                product.getBrandId(),
                product.categoryIds(),
                skuIds,
                images.isEmpty() ? null : images.get(0),
                cheapest,
                currency);
    }
}
