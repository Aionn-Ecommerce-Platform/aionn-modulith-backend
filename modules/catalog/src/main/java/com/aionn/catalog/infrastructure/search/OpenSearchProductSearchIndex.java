package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "catalog.search", name = "provider", havingValue = "opensearch")
public class OpenSearchProductSearchIndex implements ProductSearchIndex {

    private static final String FACET_BRANDS = "brands";
    private static final String FACET_CATEGORIES = "categories";
    private static final String FACET_PRICE_MIN = "priceMin";
    private static final String FACET_PRICE_MAX = "priceMax";
    private static final String FACET_ATTR_PREFIX = "attr_";
    private static final String FIELD_PRICE_FROM = "priceFrom";
    private static final String FIELD_BRAND_ID = "brandId";
    private static final String FIELD_CATEGORY_IDS = "categoryIds";

    private final OpenSearchClient client;
    private final CatalogSearchProperties searchProperties;

    private String index() {
        return searchProperties.opensearch().indexName();
    }

    @Override
    public void index(ProductSearchDocument document) {
        try {
            client.index(IndexRequest.of(req -> req
                    .index(index())
                    .id(document.productId())
                    .document(document)));
        } catch (IOException ex) {
            throw new IllegalStateException("OpenSearch index failed", ex);
        }
    }

    @Override
    public void indexAll(List<ProductSearchDocument> documents) {
        if (documents.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder bulk = new BulkRequest.Builder();
            for (ProductSearchDocument doc : documents) {
                bulk.operations(op -> op.index(idx -> idx
                        .index(index())
                        .id(doc.productId())
                        .document(doc)));
            }
            BulkResponse response = client.bulk(bulk.build());
            if (response.errors()) {
                response.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("OpenSearch bulk index failed for id={} status={} reason={}",
                                item.id(), item.status(), item.error().reason());
                    }
                });
            }
        } catch (IOException ex) {
            throw new IllegalStateException("OpenSearch bulk index failed", ex);
        }
    }

    @Override
    public void remove(String productId) {
        try {
            client.delete(DeleteRequest.of(req -> req.index(index()).id(productId)));
        } catch (IOException ex) {
            throw new IllegalStateException("OpenSearch delete failed", ex);
        }
    }

    @Override
    public void removeAll(List<String> productIds) {
        productIds.forEach(this::remove);
    }

    @Override
    public Optional<SearchHits> search(ProductSearchCriteria criteria) {
        try {
            Query mainQuery = Query.of(q -> q.bool(b -> {
                b.filter(buildFilters(criteria));
                if (criteria.hasText()) {
                    b.must(m -> m.multiMatch(mm -> mm
                            .query(criteria.q())
                            .fields("name^3", "aiDescription", "tags")
                            .fuzziness("AUTO")));
                }
                return b;
            }));

            SearchRequest.Builder reqBuilder = new SearchRequest.Builder()
                    .index(index())
                    .from(criteria.page() * criteria.size())
                    .size(criteria.size())
                    .query(mainQuery)
                    .aggregations(buildAggregations(criteria))
                    .source(s -> s.fetch(false))
                    .trackTotalHits(t -> t.enabled(true));
            applySort(reqBuilder, criteria);

            SearchResponse<ProductSearchDocument> response = client.search(
                    reqBuilder.build(), ProductSearchDocument.class);
            return Optional.of(toSearchHits(response, criteria));
        } catch (IOException | RuntimeException ex) {
            log.warn("OpenSearch search failed, returning empty optional: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private List<Query> buildFilters(ProductSearchCriteria criteria) {
        List<Query> filters = new ArrayList<>();
        String statusValue = criteria.status() != null ? criteria.status().name() : "PUBLISHED";
        filters.add(termFilter("status", statusValue));
        if (criteria.merchantId() != null && !criteria.merchantId().isBlank()) {
            filters.add(termFilter("merchantId", criteria.merchantId()));
        }
        if (!criteria.brandIds().isEmpty()) {
            filters.add(termsFilter(FIELD_BRAND_ID, criteria.brandIds()));
        }
        if (!criteria.categoryIds().isEmpty()) {
            filters.add(termsFilter(FIELD_CATEGORY_IDS, criteria.categoryIds()));
        }
        if (criteria.priceMin() != null || criteria.priceMax() != null) {
            filters.add(Query.of(q -> q.range(r -> {
                r.field(FIELD_PRICE_FROM);
                if (criteria.priceMin() != null) {
                    r.gte(JsonData.of(criteria.priceMin()));
                }
                if (criteria.priceMax() != null) {
                    r.lte(JsonData.of(criteria.priceMax()));
                }
                return r;
            })));
        }
        if (criteria.ratingMin() != null) {
            filters.add(Query.of(q -> q.range(r -> r.field("rating").gte(JsonData.of(criteria.ratingMin())))));
        }
        criteria.attributes().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                filters.add(termsFilter("filterableAttributes." + key + ".keyword", values));
            }
        });
        return filters;
    }

    private static Query termFilter(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private static Query termsFilter(String field, List<String> values) {
        return Query.of(q -> q.terms(t -> t.field(field)
                .terms(v -> v.value(values.stream().map(FieldValue::of).toList()))));
    }

    private Map<String, Aggregation> buildAggregations(ProductSearchCriteria criteria) {
        Map<String, Aggregation> aggs = new LinkedHashMap<>();
        aggs.put(FACET_BRANDS, Aggregation.of(a -> a.terms(t -> t.field(FIELD_BRAND_ID).size(50))));
        aggs.put(FACET_CATEGORIES, Aggregation.of(a -> a.terms(t -> t.field(FIELD_CATEGORY_IDS).size(100))));
        aggs.put(FACET_PRICE_MIN, Aggregation.of(a -> a.min(m -> m.field(FIELD_PRICE_FROM))));
        aggs.put(FACET_PRICE_MAX, Aggregation.of(a -> a.max(m -> m.field("priceTo"))));
        for (String attrKey : criteria.attributes().keySet()) {
            aggs.put(FACET_ATTR_PREFIX + attrKey, Aggregation.of(a -> a.terms(t -> t
                    .field("filterableAttributes." + attrKey + ".keyword").size(50))));
        }
        return aggs;
    }

    private static void applySort(SearchRequest.Builder reqBuilder, ProductSearchCriteria criteria) {
        switch (criteria.sort()) {
            case PRICE_ASC -> reqBuilder.sort(s -> s.field(f -> f.field(FIELD_PRICE_FROM).order(SortOrder.Asc)));
            case PRICE_DESC -> reqBuilder.sort(s -> s.field(f -> f.field(FIELD_PRICE_FROM).order(SortOrder.Desc)));
            case NEWEST -> reqBuilder.sort(s -> s.field(f -> f.field("updatedAt").order(SortOrder.Desc)));
            case BEST_SELLER -> reqBuilder.sort(s -> s.field(f -> f.field("soldCount").order(SortOrder.Desc)));
            case RELEVANCE -> {
                if (!criteria.hasText()) {
                    reqBuilder.sort(s -> s.field(f -> f.field("updatedAt").order(SortOrder.Desc)));
                }
            }
        }
    }

    private static SearchHits toSearchHits(SearchResponse<ProductSearchDocument> response,
            ProductSearchCriteria criteria) {
        List<String> ids = response.hits().hits().stream().map(Hit::id).toList();
        long total = response.hits().total() == null ? 0L : response.hits().total().value();

        Map<String, Long> brandCounts = readTermBuckets(response.aggregations().get(FACET_BRANDS));
        Map<String, Long> categoryCounts = readTermBuckets(response.aggregations().get(FACET_CATEGORIES));

        Map<String, Map<String, Long>> attrCounts = new LinkedHashMap<>();
        for (String attrKey : criteria.attributes().keySet()) {
            Aggregate agg = response.aggregations().get(FACET_ATTR_PREFIX + attrKey);
            if (agg != null) {
                attrCounts.put(attrKey, readTermBuckets(agg));
            }
        }

        BigDecimal priceMin = readMinAgg(response.aggregations().get(FACET_PRICE_MIN));
        BigDecimal priceMax = readMaxAgg(response.aggregations().get(FACET_PRICE_MAX));
        return new SearchHits(ids, total, brandCounts, categoryCounts, attrCounts, priceMin, priceMax);
    }

    private static Map<String, Long> readTermBuckets(Aggregate agg) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (agg == null) {
            return out;
        }
        if (agg.isSterms()) {
            for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
                out.put(bucket.key(), bucket.docCount());
            }
        } else if (agg.isLterms()) {
            agg.lterms().buckets().array().forEach(b -> out.put(String.valueOf(b.key()), b.docCount()));
        }
        return out;
    }

    private static BigDecimal readMinAgg(Aggregate agg) {
        if (agg == null || !agg.isMin()) {
            return null;
        }
        return finiteOrNull(agg.min().value());
    }

    private static BigDecimal readMaxAgg(Aggregate agg) {
        if (agg == null || !agg.isMax()) {
            return null;
        }
        return finiteOrNull(agg.max().value());
    }

    private static BigDecimal finiteOrNull(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }
}
