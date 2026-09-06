package com.aionn.sharedkernel.integration.port.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Classification-oriented product lookup for behavioural and content-based features.
 *
 * <p>Separate from {@link CatalogQueryPort}, which deliberately exposes only a flat purchasable view
 * (name, images, variants) and omits classification. Consumers here need the taxonomy - brand,
 * categories, price - to reason about similarity, so widening that port would blur its contract.
 *
 * <p>Only published products are returned; taken-down and unpublished ones are filtered by the
 * implementation.
 */
public interface CatalogProductAttributeQueryPort {

    Optional<ProductAttributeView> findByProductId(String productId);

    /** Bulk lookup. Products that do not exist or are not published are simply absent from the map. */
    Map<String, ProductAttributeView> findByProductIds(Collection<String> productIds);

    /** Resolves a SKU to its owning product, for signals that arrive SKU-scoped. */
    Optional<String> findProductIdBySkuId(String skuId);

    /** Bulk SKU resolution, keyed by the input SKU ID. Unmatched SKUs are absent. */
    Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds);

    /**
     * Published products matching any of the given categories or brands. Catalog owns the filtering so
     * callers never query its tables directly.
     */
    List<ProductAttributeView> findByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit);

    /** Most recently published products, newest first. Cold-start pool for empty behavioural data. */
    List<ProductAttributeView> findRecentlyPublished(int limit);

    /**
     * @param priceFrom cheapest variant price, or null when no variant is priced
     * @param skuIds    every variant SKU, so callers can check fulfilability against inventory
     */
    record ProductAttributeView(
            String productId,
            String name,
            String brandId,
            List<String> categoryIds,
            List<String> skuIds,
            String imageUrl,
            BigDecimal priceFrom,
            String currency) {

        public ProductAttributeView {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
            skuIds = skuIds == null ? List.of() : List.copyOf(skuIds);
        }
    }
}
