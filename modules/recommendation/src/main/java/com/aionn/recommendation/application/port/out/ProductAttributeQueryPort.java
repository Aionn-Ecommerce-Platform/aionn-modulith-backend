package com.aionn.recommendation.application.port.out;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Product facts the module needs from catalog: attributes for content scoring, and display fields for
 * hydrating a slate. Wraps the shared catalog query port so application code depends on a
 * recommendation-shaped contract rather than on another module's view type.
 */
public interface ProductAttributeQueryPort {

    Optional<ProductAttributes> findByProductId(String productId);

    /** Bulk lookup; products that no longer exist or are taken down are simply absent. */
    Map<String, ProductAttributes> findByProductIds(Collection<String> productIds);

    /** Resolves a SKU to its owning product, for signals that arrive SKU-scoped. */
    Optional<String> findProductIdBySkuId(String skuId);

    /** Bulk SKU resolution, keyed by the input SKU ID. Unmatched SKUs are absent. */
    Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds);

    /**
     * Candidate pool for content-based matching. Catalog owns the filtering, so recommendation never
     * queries another module's tables.
     */
    List<ProductAttributes> findCandidatesByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit);

    List<ProductAttributes> findRecentlyPublished(int limit);

    record ProductAttributes(
            String productId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> skuIds,
            String imageUrl,
            BigDecimal priceFrom,
            String currency) {

        public ProductAttributes {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
            skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
        }
    }
}
