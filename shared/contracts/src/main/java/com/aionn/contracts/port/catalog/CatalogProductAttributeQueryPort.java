package com.aionn.contracts.port.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CatalogProductAttributeQueryPort {

    Optional<ProductAttributeView> findByProductId(String productId);

    Map<String, ProductAttributeView> findByProductIds(Collection<String> productIds);

    Optional<String> findProductIdBySkuId(String skuId);

    Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds);

    List<ProductAttributeView> findByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit);

    List<ProductAttributeView> findRecentlyPublished(int limit);

    record ProductAttributeView(
            String productId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> skuIds,
            String imageUrl,
            BigDecimal priceFrom,
            BigDecimal originalPriceFrom,
            String currency,
            Double rating,
            Long reviewCount,
            Long soldCount,
            String provinceCode,
            String provinceName) {

        public ProductAttributeView(
                String productId,
                String name,
                String brandId,
                List<String> categoryIds,
                List<String> skuIds,
                String imageUrl,
                BigDecimal priceFrom,
                BigDecimal originalPriceFrom,
                String currency) {
            this(productId, name, brandId, categoryIds, skuIds, imageUrl, priceFrom,
                    originalPriceFrom, currency, null, null, 0L, null, null);
        }

        public ProductAttributeView(
                String productId,
                String name,
                String brandId,
                List<String> categoryIds,
                List<String> skuIds,
                String imageUrl,
                BigDecimal priceFrom,
                BigDecimal originalPriceFrom,
                String currency,
                Double rating,
                Long reviewCount,
                Long soldCount) {
            this(productId, name, brandId, categoryIds, skuIds, imageUrl, priceFrom,
                    originalPriceFrom, currency, rating, reviewCount, soldCount, null, null);
        }

        public ProductAttributeView {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
            skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
        }
    }
}
