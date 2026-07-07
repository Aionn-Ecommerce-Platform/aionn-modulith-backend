package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Default faceted search backend. Keeps documents in memory and computes
// filters, sorting and facet aggregations locally. Used unless an OpenSearch
// backend is enabled via catalog.search.provider=opensearch.
@Component
@ConditionalOnProperty(prefix = "catalog.search", name = "provider", havingValue = "in-process", matchIfMissing = true)
public class InMemoryFacetProductSearchIndex implements ProductSearchIndex {

    private final Map<String, ProductSearchDocument> byId = new ConcurrentHashMap<>();

    @Override
    public void index(ProductSearchDocument document) {
        byId.put(document.productId(), document);
    }

    @Override
    public void indexAll(List<ProductSearchDocument> documents) {
        if (documents == null) {
            return;
        }
        documents.forEach(this::index);
    }

    @Override
    public void remove(String productId) {
        byId.remove(productId);
    }

    @Override
    public void removeAll(List<String> productIds) {
        if (productIds == null) {
            return;
        }
        productIds.forEach(byId::remove);
    }

    @Override
    public Optional<SearchHits> search(ProductSearchCriteria criteria) {
        List<ProductSearchDocument> matched = byId.values().stream()
                .filter(doc -> matches(doc, criteria))
                .sorted(comparator(criteria.sort()))
                .toList();

        Map<String, Long> brandCounts = new LinkedHashMap<>();
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        Map<String, Map<String, Long>> attributeCounts = new LinkedHashMap<>();
        BigDecimal priceMin = null;
        BigDecimal priceMax = null;
        for (ProductSearchDocument doc : matched) {
            if (doc.brandId() != null) {
                brandCounts.merge(doc.brandId(), 1L, Long::sum);
            }
            for (String cat : doc.categoryIds()) {
                categoryCounts.merge(cat, 1L, Long::sum);
            }
            doc.filterableAttributes().forEach((key, value) -> attributeCounts
                    .computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .merge(value, 1L, Long::sum));
            if (doc.priceFrom() != null) {
                priceMin = priceMin == null ? doc.priceFrom() : priceMin.min(doc.priceFrom());
                priceMax = priceMax == null ? doc.priceFrom() : priceMax.max(doc.priceFrom());
            }
        }

        int from = Math.min(criteria.page() * criteria.size(), matched.size());
        int to = Math.min(from + criteria.size(), matched.size());
        List<String> pageIds = matched.subList(from, to).stream()
                .map(ProductSearchDocument::productId)
                .toList();

        return Optional.of(new SearchHits(
                pageIds, matched.size(), brandCounts, categoryCounts, attributeCounts, priceMin, priceMax));
    }

    private static boolean matches(ProductSearchDocument doc, ProductSearchCriteria criteria) {
        String requiredStatus = criteria.status() == null ? "PUBLISHED" : criteria.status().name();
        if (!requiredStatus.equals(doc.status())) {
            return false;
        }
        if (criteria.merchantId() != null && !criteria.merchantId().isBlank()
                && !criteria.merchantId().equals(doc.merchantId())) {
            return false;
        }
        if (criteria.hasText() && !matchesText(doc, criteria.q())) {
            return false;
        }
        if (!criteria.brandIds().isEmpty() && (doc.brandId() == null || !criteria.brandIds().contains(doc.brandId()))) {
            return false;
        }
        if (!criteria.categoryIds().isEmpty()
                && doc.categoryIds().stream().noneMatch(criteria.categoryIds()::contains)) {
            return false;
        }
        if (!matchesPrice(doc, criteria)) {
            return false;
        }
        if (!matchesAttributes(doc, criteria)) {
            return false;
        }
        return criteria.ratingMin() == null
                || (doc.rating() != null && doc.rating() >= criteria.ratingMin());
    }

    private static boolean matchesText(ProductSearchDocument doc, String q) {
        String needle = q.trim().toLowerCase(Locale.ROOT);
        StringBuilder haystack = new StringBuilder();
        if (doc.name() != null) {
            haystack.append(doc.name().toLowerCase(Locale.ROOT)).append(' ');
        }
        if (doc.aiDescription() != null) {
            haystack.append(doc.aiDescription().toLowerCase(Locale.ROOT)).append(' ');
        }
        doc.tags().forEach(t -> haystack.append(t.toLowerCase(Locale.ROOT)).append(' '));
        return haystack.toString().contains(needle);
    }

    private static boolean matchesPrice(ProductSearchDocument doc, ProductSearchCriteria criteria) {
        if (criteria.priceMin() == null && criteria.priceMax() == null) {
            return true;
        }
        if (doc.priceFrom() == null) {
            return false;
        }
        if (criteria.priceMin() != null && doc.priceFrom().compareTo(criteria.priceMin()) < 0) {
            return false;
        }
        return criteria.priceMax() == null || doc.priceFrom().compareTo(criteria.priceMax()) <= 0;
    }

    private static boolean matchesAttributes(ProductSearchDocument doc, ProductSearchCriteria criteria) {
        for (Map.Entry<String, List<String>> entry : criteria.attributes().entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String value = doc.filterableAttributes().get(entry.getKey());
            if (value == null || !entry.getValue().contains(value)) {
                return false;
            }
        }
        return true;
    }

    private static Comparator<ProductSearchDocument> comparator(ProductSearchCriteria.Sort sort) {
        return switch (sort) {
            case NEWEST -> Comparator.comparing(ProductSearchDocument::updatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case PRICE_ASC -> Comparator.comparing(ProductSearchDocument::priceFrom,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICE_DESC -> Comparator.comparing(ProductSearchDocument::priceFrom,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case BEST_SELLER -> Comparator.comparing(ProductSearchDocument::soldCount,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case RELEVANCE -> Comparator.comparing(ProductSearchDocument::productId);
        };
    }
}
