package com.aionn.sharedkernel.integration.port.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Cross-service catalog query port. Returns flat, transport-friendly views of
// products + variants so callers (UCP, recommendations, search aggregators)
// never see catalog's domain types.
// The ProductView record intentionally omits status — only purchasable products
// surface through this port. Variants and media keep their order; the
// implementation is responsible for filtering taken-down products and applying
// any privacy rules.
public interface CatalogQueryPort {

        // Free-text + price-range search. Returns at most limit products, starting at
        // offset 0.
        List<ProductView> search(SearchCriteria criteria);

        // Resolve product by id, or by SKU id when the caller doesn't know which
        // shape it has. Returns the owning product in either case.
        Optional<ProductView> findByProductOrSkuId(String id);

        // Bulk lookup. Each input id may be either a product id or a SKU id; the
        // port resolves both and reports inputs it couldn't match.
        LookupResult lookupByProductOrSkuIds(List<String> ids);

        // Filter passed straight from UCP search using major-unit decimal prices.
        record SearchCriteria(String query, int limit, BigDecimal minPrice, BigDecimal maxPrice) {
        }

        record LookupResult(List<ProductView> products, List<String> notFound) {
        }

        record ProductView(
                        String productId,
                        String name,
                        String description,
                        List<String> imageUrls,
                        List<VariantView> variants) {
        }

        record VariantView(
                        String skuId,
                        String displayName,
                        BigDecimal price,
                        String currency,
                        boolean available,
                        Map<String, String> attributeValues) {
        }
}
