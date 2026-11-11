package com.aionn.contracts.port.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Cross-service catalog query port. Returns flat, transport-friendly views so callers never see
// catalog domain types. Only purchasable products surface; the implementation filters taken-down ones.
public interface CatalogQueryPort {

        List<ProductView> search(SearchCriteria criteria);

        /** Resolve by product id or SKU id. */
        Optional<ProductView> findByProductOrSkuId(String id);

        /** Bulk lookup of product-or-SKU ids; reports inputs it couldn't match. */
        LookupResult lookupByProductOrSkuIds(List<String> ids);

        /** Filter passed from UCP search using major-unit decimal prices. */
        record SearchCriteria(String query, int limit, BigDecimal minPrice, BigDecimal maxPrice, int offset) {
                public SearchCriteria(String query, int limit, BigDecimal minPrice, BigDecimal maxPrice) {
                        this(query, limit, minPrice, maxPrice, 0);
                }
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
                        Map<String, String> attributeValues,
                        ParcelView parcel) {
                public VariantView(String skuId, String displayName, BigDecimal price, String currency,
                                boolean available, Map<String, String> attributeValues) {
                        this(skuId, displayName, price, currency, available, attributeValues, null);
                }
        }

        /** Weight is required before publish; the box is optional but complete. Null on legacy rows. */
        record ParcelView(
                        Integer weightGram,
                        BigDecimal lengthCm,
                        BigDecimal widthCm,
                        BigDecimal heightCm) {
                public boolean hasWeight() {
                        return weightGram != null && weightGram > 0;
                }
        }
}
