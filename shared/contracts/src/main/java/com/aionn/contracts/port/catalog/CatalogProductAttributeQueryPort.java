package com.aionn.contracts.port.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Classification-oriented product lookup for behavioural/content features, separate from
 * {@link CatalogQueryPort}. Only published products are returned.
 */
public interface CatalogProductAttributeQueryPort {

    Optional<ProductAttributeView> findByProductId(String productId);

    /** Bulk lookup. Products that do not exist or are not published are simply absent from the map. */
    Map<String, ProductAttributeView> findByProductIds(Collection<String> productIds);

    /** Resolves a SKU to its owning product, for signals that arrive SKU-scoped. */
    Optional<String> findProductIdBySkuId(String skuId);

    /** Bulk SKU resolution, keyed by the input SKU ID. Unmatched SKUs are absent. */
    Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds);

    /** Published products matching any category or brand; catalog owns the filtering. */
    List<ProductAttributeView> findByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit);

    /** Most recently published products, newest first. Cold-start pool for empty behavioural data. */
    List<ProductAttributeView> findRecentlyPublished(int limit);

    /**
     * @param priceFrom         cheapest variant price
     * @param originalPriceFrom list price before discount, higher than {@code priceFrom} on sale
     * @param skuIds            every variant SKU, for stock checks
     */
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
            Long soldCount) {

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
                    originalPriceFrom, currency, null, null, 0L);
        }

        public ProductAttributeView {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
            skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
        }
    }
}
