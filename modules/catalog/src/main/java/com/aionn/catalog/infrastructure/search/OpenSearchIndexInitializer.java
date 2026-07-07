package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.infrastructure.config.properties.CatalogSearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.analysis.Analyzer;
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer;
import org.opensearch.client.opensearch._types.analysis.EdgeNGramTokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;
import org.opensearch.client.opensearch._types.mapping.DateProperty;
import org.opensearch.client.opensearch._types.mapping.DoubleNumberProperty;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.LongNumberProperty;
import org.opensearch.client.opensearch._types.mapping.ObjectProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "catalog.search", name = "provider", havingValue = "opensearch")
@Order(0)
public class OpenSearchIndexInitializer {

    private final OpenSearchClient client;
    private final CatalogSearchProperties searchProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexExists() {
        String indexName = searchProperties.opensearch().indexName();
        try {
            boolean exists = client.indices()
                    .exists(ExistsRequest.of(req -> req.index(indexName)))
                    .value();
            if (exists) {
                log.info("OpenSearch index '{}' already exists", indexName);
                return;
            }
            client.indices().create(CreateIndexRequest.of(req -> req
                    .index(indexName)
                    .settings(buildSettings())
                    .mappings(buildMappings())));
            log.info("OpenSearch index '{}' created with custom mappings", indexName);
        } catch (IOException ex) {
            log.error("Failed to ensure OpenSearch index '{}' exists; search may be degraded", indexName, ex);
        } catch (RuntimeException ex) {
            log.error("Unexpected error while ensuring OpenSearch index '{}'", indexName, ex);
        }
    }

    private IndexSettings buildSettings() {
        TokenFilter edgeNgram = new TokenFilter.Builder()
                .definition(TokenFilterDefinition.of(b -> b
                        .edgeNgram(EdgeNGramTokenFilter.of(en -> en.minGram(2).maxGram(15)))))
                .build();

        Analyzer textAnalyzer = Analyzer.of(b -> b
                .custom(CustomAnalyzer.of(c -> c
                        .tokenizer("standard")
                        .filter("lowercase", "asciifolding"))));
        Analyzer autocompleteAnalyzer = Analyzer.of(b -> b
                .custom(CustomAnalyzer.of(c -> c
                        .tokenizer("standard")
                        .filter("lowercase", "asciifolding", "aionn_edge_ngram"))));

        IndexSettingsAnalysis analysis = IndexSettingsAnalysis.of(b -> b
                .filter("aionn_edge_ngram", edgeNgram)
                .analyzer("aionn_text", textAnalyzer)
                .analyzer("aionn_autocomplete", autocompleteAnalyzer));

        return IndexSettings.of(b -> b
                .numberOfShards("1")
                .numberOfReplicas("0")
                .analysis(analysis));
    }

    private TypeMapping buildMappings() {
        Property keyword = Property.of(b -> b.keyword(KeywordProperty.of(k -> k)));
        Property keywordNoIndex = Property.of(b -> b.keyword(KeywordProperty.of(k -> k.index(false))));
        Property doubleP = Property.of(b -> b.double_(DoubleNumberProperty.of(d -> d)));
        Property longP = Property.of(b -> b.long_(LongNumberProperty.of(d -> d)));
        Property dateP = Property.of(b -> b.date(DateProperty.of(d -> d)));
        Property attributesObj = Property.of(b -> b.object(ObjectProperty.of(o -> o
                .dynamic(org.opensearch.client.opensearch._types.mapping.DynamicMapping.True))));

        Property nameField = Property.of(b -> b.text(TextProperty.of(t -> t
                .analyzer("aionn_text")
                .fields("autocomplete", Property.of(p -> p.text(TextProperty.of(at -> at
                        .analyzer("aionn_autocomplete")
                        .searchAnalyzer("aionn_text")))))
                .fields("raw", keyword))));
        Property aiDescField = Property.of(b -> b.text(TextProperty.of(t -> t.analyzer("aionn_text"))));

        return TypeMapping.of(b -> b
                .properties("productId", keyword)
                .properties("merchantId", keyword)
                .properties("name", nameField)
                .properties("aiDescription", aiDescField)
                .properties("brandId", keyword)
                .properties("categoryIds", keyword)
                .properties("collectionIds", keyword)
                .properties("tags", keyword)
                .properties("imageList", keywordNoIndex)
                .properties("filterableAttributes", attributesObj)
                .properties("priceFrom", doubleP)
                .properties("priceTo", doubleP)
                .properties("currency", keyword)
                .properties("status", keyword)
                .properties("updatedAt", dateP)
                .properties("rating", doubleP)
                .properties("soldCount", longP));
    }
}
