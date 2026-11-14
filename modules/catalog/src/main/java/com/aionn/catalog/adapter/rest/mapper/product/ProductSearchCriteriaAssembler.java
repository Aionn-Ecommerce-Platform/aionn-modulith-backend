package com.aionn.catalog.adapter.rest.mapper.product;

import com.aionn.catalog.adapter.rest.dto.product.request.SearchCatalogRequest;
import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Translates the catalogue-search query string into the application criteria. Transport concerns -
 * the {@code attr.*} facet convention and the human-readable sort token - stay in the adapter.
 */
@Component
public class ProductSearchCriteriaAssembler {

    private static final String ATTRIBUTE_PREFIX = "attr.";

    public ProductSearchCriteria toCriteria(SearchCatalogRequest request) {
        return new ProductSearchCriteria(
                request.q(), request.merchantId(), ProductStatus.PUBLISHED,
                request.categoryIds(), request.brandIds(),
                request.priceMin(), request.priceMax(), attributes(request.allParams()),
                parseSort(request.sort()), request.page(), request.size(),
                request.ratingMin(), request.onSale(), request.collectionIds());
    }

    private static ProductSearchCriteria.Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return ProductSearchCriteria.Sort.RELEVANCE;
        }
        try {
            return ProductSearchCriteria.Sort.valueOf(sort.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ProductSearchCriteria.Sort.RELEVANCE;
        }
    }

    private static Map<String, List<String>> attributes(Map<String, String> allParams) {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        if (allParams == null) {
            return attributes;
        }
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey() == null
                    || !entry.getKey().startsWith(ATTRIBUTE_PREFIX)
                    || entry.getValue() == null
                    || entry.getValue().isBlank()) {
                continue;
            }
            List<String> values = Arrays.stream(entry.getValue().split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .toList();
            if (!values.isEmpty()) {
                attributes.put(entry.getKey().substring(ATTRIBUTE_PREFIX.length()), values);
            }
        }
        return attributes;
    }
}
